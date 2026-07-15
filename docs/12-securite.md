---
noteId: "38daec607f8711f1878859078c773cc2"
tags: []

---

# 12. Sécurité

## 12.1 Vue d'ensemble — couches de défense

La sécurité est construite en couches (défense en profondeur). Chaque couche est indépendante : si l'une est compromise, les autres tiennent.

```
┌─────────────────────────────────────────────────────────────────┐
│                  Couches de sécurité                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Cloudflare (périmètre réseau)                               │
│     DDoS protection, WAF, rate limiting, TLS terminaison        │
│                                │                                │
│  2. Cilium NetworkPolicy (réseau K8s)                           │
│     Isolation inter-namespaces, eBPF L7                         │
│                                │                                │
│  3. TLS interne (mTLS optionnel)                                │
│     cert-manager, Let's Encrypt wildcard                        │
│                                │                                │
│  4. RBAC Kubernetes                                             │
│     Principe du moindre privilège par ServiceAccount            │
│                                │                                │
│  5. Sealed Secrets                                              │
│     Secrets chiffrés avant commit Git                           │
│                                │                                │
│  6. Spring Security (application)                               │
│     JWT, validation entrées, CORS, headers HTTP                 │
│                                │                                │
│  7. PostgreSQL (données)                                        │
│     Utilisateur dédié par tenant, pas de superuser              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 12.2 Sealed Secrets — secrets GitOps-safe

Les Secrets Kubernetes contiennent des données sensibles (clés API, mots de passe DB). Ils ne peuvent pas être commités en clair dans Git.

**Sealed Secrets** (Bitnami) chiffre le Secret côté développeur. Seul le contrôleur dans le cluster peut le déchiffrer.

```
Développeur                    Cluster K8s
───────────                    ───────────
Secret en clair                SealedSecret Controller
      │                              │  (clé privée dans le cluster)
      │  kubeseal --cert pubkey.pem  │
      ▼                              │
SealedSecret (chiffré)  ──────────► Décrypte → Secret K8s
(safe à commiter dans Git)           (jamais exposé hors cluster)
```

### Créer un Sealed Secret

```bash
# 1. Récupérer la clé publique du cluster
kubeseal --fetch-cert \
  --controller-name=sealed-secrets \
  --controller-namespace=platform \
  > pubkey.pem

# 2. Créer le Secret en clair (jamais commité)
kubectl create secret generic tenant-secrets \
  --namespace agence-lyon \
  --from-literal=STRIPE_SECRET_KEY="sk_live_xxx" \
  --from-literal=DB_PASSWORD="motdepasse-fort" \
  --dry-run=client \
  -o yaml > /tmp/secret.yaml

# 3. Chiffrer avec la clé publique du cluster
kubeseal --cert pubkey.pem \
  --format yaml \
  < /tmp/secret.yaml \
  > autoeecoleconnect-infra/tenants/agence-lyon/sealed-secret.yaml

# 4. Supprimer le secret en clair
rm /tmp/secret.yaml

# 5. Commiter le Sealed Secret (safe)
git add autoeecoleconnect-infra/tenants/agence-lyon/sealed-secret.yaml
git commit -m "feat: add secrets for agence-lyon"
```

```yaml
# autoeecoleconnect-infra/tenants/agence-lyon/sealed-secret.yaml
# Ce fichier peut être commité en toute sécurité
apiVersion: bitnami.com/v1alpha1
kind: SealedSecret
metadata:
  name: tenant-secrets
  namespace: agence-lyon
spec:
  encryptedData:
    STRIPE_SECRET_KEY: AgBx3kR...  # chiffré RSA-OAEP
    DB_PASSWORD: AgCy7mP...
    PAYPLUG_SECRET_KEY: AgDz9nQ...
  template:
    metadata:
      name: tenant-secrets
      namespace: agence-lyon
    type: Opaque
```

---

## 12.3 Cilium NetworkPolicy — isolation réseau

Déjà présentée en section 8.5, voici le raisonnement de sécurité derrière chaque règle :

```yaml
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: tenant-isolation
  namespace: agence-lyon
spec:
  endpointSelector: {}    # s'applique à tous les pods du namespace

  ingress:
    # Autoriser uniquement le trafic entrant depuis Cilium Gateway
    - fromEntities: [cluster]
      toPorts:
        - ports:
            - port: "3000"    # frontend Next.js
            - port: "8080"    # backend Spring Boot

    # Autoriser Prometheus à scraper les métriques
    - fromNamespaces:
        - matchLabels:
            kubernetes.io/metadata.name: monitoring

  egress:
    # Le backend peut parler à son propre PostgreSQL (même namespace)
    - toEndpoints:
        - matchLabels: {app: postgres}

    # Le backend peut publier des événements dans RabbitMQ (namespace platform)
    - toNamespaces:
        - matchLabels:
            kubernetes.io/metadata.name: platform
      toPorts:
        - ports:
            - port: "5672"    # AMQP RabbitMQ

    # Sortie HTTPS uniquement (Stripe, PayPlug, Resend...)
    - toEntities: [world]
      toPorts:
        - ports:
            - port: "443"
```

**Ce que cette policy interdit implicitement :**
- Un pod de `agence-lyon` ne peut pas contacter un pod de `agence-paris` (isolation totale entre tenants)
- Pas d'accès à la BDD Control Plane depuis un tenant
- Pas de sortie sur le port 80 (HTTP non-chiffré vers l'extérieur)
- Pas d'accès aux métadonnées cloud (169.254.169.254 bloqué)

---

## 12.4 RBAC Kubernetes — principe du moindre privilège

Chaque composant a exactement les droits dont il a besoin — rien de plus.

```yaml
# Control Plane — droits pour gérer les tenants
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: tenant-manager
rules:
  - apiGroups: [""]
    resources: ["namespaces"]
    verbs: ["get", "list", "create", "delete"]
  - apiGroups: ["apps"]
    resources: ["deployments", "deployments/scale"]
    verbs: ["get", "list", "patch", "update"]
  - apiGroups: ["batch"]
    resources: ["jobs"]
    verbs: ["get", "list", "create"]
---
# Backend tenant — aucun droit K8s (accès BDD seul)
apiVersion: v1
kind: ServiceAccount
metadata:
  name: backend
  namespace: agence-lyon
# Pas de RoleBinding → le pod backend ne peut rien faire dans K8s
# Il accède uniquement à sa BDD PostgreSQL via les variables d'env
```

**Règle d'or :** un pod applicatif (frontend, backend tenant) ne doit jamais avoir accès à l'API K8s. Seul le Control Plane et les opérateurs ont des droits K8s.

---

## 12.5 Spring Security — JWT + autorisation

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())          // API REST — pas de CSRF
            .sessionManagement(session ->
                session.sessionCreationPolicy(STATELESS))   // JWT — pas de session
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/forfaits").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Tout le reste requiert un JWT valide
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Vérifie la signature avec la clé secrète du tenant
        return NimbusJwtDecoder
            .withSecretKey(tenantJwtKey())
            .build();
    }
}
```

### Validation des entrées

```java
// DTO avec validation Bean Validation
public record ReservationRequest(
    @NotNull UUID moniteurId,
    @NotNull @Future LocalDateTime debut,     // doit être dans le futur
    @NotBlank @Size(max = 500) String notes
) {}

// Controller — Spring valide automatiquement si @Valid
@PostMapping("/reservations")
public ResponseEntity<ReservationResponse> create(
    @Valid @RequestBody ReservationRequest request,
    Authentication auth
) {
    // Si la validation échoue → 400 automatique, sans toucher au service
    return ResponseEntity.ok(service.create(request, auth));
}
```

### Headers de sécurité HTTP

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .headers(headers -> headers
            .contentSecurityPolicy(csp ->
                csp.policyDirectives("default-src 'self'; script-src 'self'"))
            .frameOptions(frame -> frame.deny())         // anti-clickjacking
            .xssProtection(xss -> xss.enable())
            .httpStrictTransportSecurity(hsts ->
                hsts.maxAgeInSeconds(31536000)           // HSTS 1 an
                    .includeSubDomains(true))
        )
        .build();
}
```

---

## 12.6 RGPD — obligations légales

AutoEcoleConnect traite des données personnelles (nom, adresse, date de naissance des élèves). Les obligations RGPD :

| Obligation | Implémentation |
|---|---|
| Droit à l'effacement | `DELETE /api/v1/clients/{id}` → anonymise les données (ne supprime pas les reservations) |
| Droit d'accès | `GET /api/v1/clients/{id}/export` → JSON complet de toutes les données |
| Durée de conservation | Données supprimées 3 ans après fin de contrat (CronJob nightly) |
| Registre des traitements | Document `docs/rgpd/registre.md` — obligatoire pour les structures > 250 salariés |
| Chiffrement au repos | CloudNativePG chiffre le volume PostgreSQL (AES-256) |
| Logs anonymisés | Pas de données personnelles dans les logs Loki (IDs uniquement) |

```java
// Anonymisation RGPD — ne supprime pas mais efface les données personnelles
@Transactional
public void anonymiserClient(UUID clientId) {
    Client client = clientRepo.findById(clientId).orElseThrow();

    client.setNom("Anonymisé");
    client.setPrenom("Anonymisé");
    client.setEmail("anonymise-" + clientId + "@supprime.invalid");
    client.setTelephone(null);
    client.setDateNaissance(null);
    client.setAdresse(null);
    client.setAnonymisedAt(Instant.now());

    clientRepo.save(client);
    // Les réservations et paiements sont conservés pour comptabilité
}
```

---

## 12.7 Sécurité de la chaîne CI/CD

```
Risques de la chaîne CI/CD et mitigations :

1. Secret dans les logs CI
   ❌ echo $STRIPE_KEY dans un script
   ✅ GitHub Secrets masque automatiquement les valeurs connues
   ✅ Jamais de secrets dans les variables d'env nommées avec "KEY" sans GitHub Secrets

2. Image Docker avec vulnérabilités
   ✅ Trivy scan dans le pipeline CI
   ✅ Images base officielles (eclipse-temurin, node:alpine)
   ✅ Rebuild automatique des images quand la base est mise à jour (Dependabot)

3. Supply chain — dépendances compromises
   ✅ Dependabot pour les mises à jour automatiques
   ✅ npm audit / mvn dependency-check dans le CI

4. ArgoCD exposé sur Internet
   ✅ ArgoCD UI accessible uniquement via VPN ou accès IP restreint
   ✅ SSO avec GitHub OAuth pour l'authentification ArgoCD
```

```yaml
# .github/workflows/security-scan.yml — scan de vulnérabilités
name: Security Scan

on:
  push:
    branches: [main]

jobs:
  trivy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: "ghcr.io/autoeecoleconnect/backend:${{ github.sha }}"
          format: "sarif"
          output: "trivy-results.sarif"
          severity: "CRITICAL,HIGH"
          exit-code: "1"    # fail le CI si CRITICAL détecté

      - name: Upload to GitHub Security tab
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: "trivy-results.sarif"
```

---

## 🎓 Montée en compétence — Sécurité DevOps (DevSecOps)

### Les 4 attaques les plus fréquentes sur une SaaS et comment s'en protéger

```
1. Injection SQL
   Vecteur  : données utilisateur non filtrées dans une requête SQL
   Protection: JPA/Hibernate — paramètres préparés automatiquement
               Ne jamais construire des requêtes SQL par concaténation

2. IDOR (Insecure Direct Object Reference)
   Vecteur  : /api/v1/reservations/456 → retourne la réservation d'un autre tenant
   Protection: toujours vérifier que la ressource appartient au tenant authentifié
               Exemple : repo.findByIdAndTenantId(id, tenantId) — pas findById(id)

3. Secrets dans Git
   Vecteur  : clé Stripe commitée accidentellement → GitHub Secret Scanner détecte
   Protection: Sealed Secrets, .gitignore strict, pre-commit hook (detect-secrets)
               git-secrets (AWS) ou trufflehog pour scanner l'historique

4. Image Docker vulnérable
   Vecteur  : base image node:18 avec CVE connue
   Protection: Trivy dans le CI, Dependabot pour les mises à jour, distroless images
```

### Sealed Secrets vs External Secrets Operator

```
Sealed Secrets :
  + Simple — un seul outil, pas de dépendance externe
  + Le secret chiffré est dans Git (tout l'état est dans le repo)
  - Si la clé privée du cluster est perdue → secrets irrécupérables
  - Rotation d'un secret = modifier le Sealed Secret + redeployer

External Secrets Operator (ESO) :
  + Source de vérité centralisée (HashiCorp Vault, AWS Secrets Manager...)
  + Rotation de secrets sans modifier Git
  - Dépendance externe (si Vault tombe → pods ne démarrent plus)
  - Plus complexe à opérer

Choix AutoEcoleConnect : Sealed Secrets (simplicité, tout dans Git)
Migration vers ESO si > 50 tenants ou audit SOC2 requis
```

### Commandes de sécurité utiles

```bash
# Scanner une image Docker pour les CVE
trivy image ghcr.io/autoeecoleconnect/backend:latest

# Vérifier les Sealed Secrets dans le cluster
kubectl get sealedsecret -A

# Voir quel ServiceAccount utilise un pod
kubectl get pod backend-xxx -n agence-lyon -o jsonpath='{.spec.serviceAccountName}'

# Lister les RBAC d'un ServiceAccount
kubectl auth can-i --list --as=system:serviceaccount:agence-lyon:backend

# Vérifier les NetworkPolicy actives sur un namespace
kubectl get ciliumnetworkpolicy -n agence-lyon -o yaml

# Hubble — inspecter le trafic réseau en temps réel (eBPF)
hubble observe --namespace agence-lyon --follow
hubble observe --namespace agence-lyon --verdict DROPPED    # voir les flux bloqués
```

### CKS — Certified Kubernetes Security Specialist

Après le CKA, la certification CKS valide la sécurité K8s :

```
CKA (Certified Kubernetes Administrator)
  → administrer un cluster (déjà couvert dans ce projet)

CKS (Certified Kubernetes Security Specialist)
  → Network Policies, RBAC, Secrets management, image scanning,
    Pod Security Standards, Audit logging, Runtime security (Falco)
  Prérequis : CKA valide
  Durée préparation : 2-3 mois après CKA
```

**Ressources :**
- [OWASP Top 10 — les vulnérabilités web les plus critiques](https://owasp.org/www-project-top-ten/)
- [Sealed Secrets — Bitnami](https://github.com/bitnami-labs/sealed-secrets)
- [Trivy — scanner de vulnérabilités](https://trivy.dev/)
- [Kubernetes RBAC](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
- [Hubble — observabilité réseau Cilium](https://docs.cilium.io/en/stable/observability/hubble/)
- [RGPD — CNIL guide développeur](https://www.cnil.fr/fr/guide-rgpd-du-developpeur)

**Ce que ça t'apporte sur le CV :**
Comprendre et implémenter une stratégie de sécurité en couches (Cloudflare → NetworkPolicy → RBAC → Sealed Secrets → Spring Security → RGPD) montre une maturité rare chez un développeur. La plupart des devs pensent "sécurité = authentification". Montrer que tu intègres la sécurité dans chaque couche — du réseau au code applicatif jusqu'à la chaîne CI/CD — c'est le profil d'un ingénieur senior.

---

