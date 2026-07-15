---
noteId: "38da9e407f8711f1878859078c773cc2"
tags: []

---

# 4. Structure des Repos

## 4.1 Vue d'ensemble — deux repos distincts

```
GitHub
├── autoeecoleconnect/              ← repo 1 : code applicatif (monorepo)
└── autoeecoleconnect-infra/        ← repo 2 : infrastructure GitOps
```

**Pourquoi séparer app et infra ?**
- Le code applicatif et l'infrastructure évoluent à des rythmes différents
- ArgoCD surveille uniquement `autoeecoleconnect-infra/` — le code source reste privé
- Une équipe infra peut modifier les manifests K8s sans toucher au code
- L'historique Git de l'infra est un audit trail complet de toutes les opérations

---

## 4.2 Repo 1 — `autoeecoleconnect/` (monorepo applicatif)

```
autoeecoleconnect/
│
├── backend/                          ← Java 21 + Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/app/autoeecoleconnect/
│   │   │   │   ├── config/           ← Spring Security, AMQP, OpenAPI
│   │   │   │   ├── controllers/      ← REST endpoints (@RestController)
│   │   │   │   ├── services/         ← logique métier
│   │   │   │   ├── repositories/     ← Spring Data JPA
│   │   │   │   ├── models/           ← entités JPA (Client, Moniteur, etc.)
│   │   │   │   ├── events/           ← événements RabbitMQ (typés)
│   │   │   │   └── exceptions/       ← DomainException, GlobalExceptionHandler
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/changelog/     ← migrations Liquibase
│   │   │           ├── db.changelog-master.yaml
│   │   │           └── v1.0/
│   │   │               ├── 001-create-clients.sql
│   │   │               └── 002-create-reservations.sql
│   │   └── test/
│   │       └── java/app/autoeecoleconnect/
│   │           ├── controllers/      ← tests intégration (Testcontainers)
│   │           └── services/         ← tests unitaires (JUnit + Mockito)
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                         ← Next.js 15 (TypeScript)
│   ├── app/
│   │   ├── (auth)/                   ← pages login, register
│   │   ├── (admin)/                  ← espace directeur
│   │   ├── (moniteur)/               ← espace moniteur
│   │   ├── (client)/                 ← espace élève
│   │   └── api/                      ← route handlers Next.js (BFF léger)
│   ├── components/
│   ├── lib/
│   │   ├── api.ts                    ← client HTTP typé (types depuis OpenAPI)
│   │   ├── auth.ts                   ← gestion JWT
│   │   └── payment-mode.ts           ← toggle Stripe/PayPlug/Manuel
│   ├── types/
│   │   └── api.ts                    ← auto-généré depuis Spring Boot OpenAPI
│   ├── Dockerfile
│   ├── next.config.ts                ← output: 'standalone'
│   └── package.json
│
├── control-plane/                    ← app Next.js + Spring Boot (autoeecoleconnect.app)
│   ├── backend/                      ← Provisioning Service (Spring Boot)
│   │   └── src/main/java/
│   │       ├── provisioning/         ← crée namespaces K8s via GitHub API
│   │       ├── billing/              ← Stripe Subscriptions (B2B)
│   │       └── metrics/             ← agrège CA par organisation (RabbitMQ)
│   └── frontend/                     ← site vitrine + dashboard gérant
│       └── app/
│           ├── rejoindre/            ← formulaire inscription auto-école
│           ├── dashboard/            ← CA consolidé, multi-agences
│           └── admin/                ← super-admin (suspension, métriques)
│
├── docker-compose.yml                ← dev local (tous les services)
├── docker-compose.override.yml       ← surcharges locales (ports, volumes)
└── .github/
    └── workflows/
        ├── ci.yml                    ← tests + lint (toutes branches)
        ├── cd.yml                    ← build images + push GHCR + update GitOps
        └── security-scan.yml         ← scan Trivy des images
```

---

## 4.3 Repo 2 — `autoeecoleconnect-infra/` (GitOps)

```
autoeecoleconnect-infra/
│
├── terraform/                        ← provisionnement Hetzner
│   ├── main.tf                       ← serveur bare metal AX41
│   ├── dns.tf                        ← Cloudflare DNS (wildcard *.autoeecoleconnect.app)
│   ├── storage.tf                    ← Hetzner Object Storage (buckets S3)
│   ├── variables.tf
│   └── outputs.tf
│
├── ansible/                          ← configuration du serveur + K8s
│   ├── inventory/
│   │   └── hetzner.yml              ← IP du serveur bare metal
│   ├── playbooks/
│   │   ├── bootstrap-server.yml     ← OS setup, containerd, kubeadm
│   │   ├── init-cluster.yml         ← kubeadm init + Cilium
│   │   └── join-nodes.yml           ← kubeadm join (si multi-nœuds)
│   └── roles/
│       ├── common/                  ← packages, firewall, sysctl
│       ├── containerd/              ← runtime containers
│       └── kubernetes/              ← kubelet, kubeadm, kubectl
│
├── helm/
│   ├── control-plane/               ← chart Helm du Control Plane (namespace platform)
│   └── portail-tenant/              ← chart Helm pour chaque tenant
│       ├── Chart.yaml
│       ├── values.yaml              ← valeurs par défaut
│       └── templates/
│           ├── namespace.yaml
│           ├── deployment-backend.yaml
│           ├── deployment-frontend.yaml
│           ├── postgresql-cluster.yaml  ← CloudNativePG CRD
│           ├── service.yaml
│           ├── httproute.yaml           ← Cilium Gateway API
│           ├── networkpolicy.yaml       ← isolation inter-namespaces
│           ├── sealed-secret.yaml       ← credentials chiffrés
│           ├── hpa.yaml                 ← auto-scaling CPU/RAM
│           ├── keda-scaledobject.yaml   ← auto-scaling RabbitMQ
│           ├── backup-schedule.yaml     ← CloudNativePG backup S3
│           └── velero-schedule.yaml     ← snapshot namespace complet
│
├── argocd/
│   ├── projects/
│   │   └── autoeecoleconnect.yaml           ← ArgoCD Project (RBAC)
│   └── apps/
│       ├── platform.yaml                 ← Application ArgoCD (Control Plane)
│       └── applicationset-tenants.yaml   ← génère une Application par dossier tenants/
│
└── tenants/                         ← un dossier par tenant (créé par Provisioning Service)
    ├── agence-lyon/
    │   └── values.yaml              ← config spécifique Lyon
    ├── agence-paris/
    │   └── values.yaml
    └── agence-bordeaux/
        └── values.yaml
```

---

## 4.4 Exemple de `values.yaml` par tenant

```yaml
# autoeecoleconnect-infra/tenants/agence-lyon/values.yaml

tenant:
  slug: agence-lyon
  nom: "Auto-École Lyon Centre"
  plan: pro                           # solo | pro | groupe | reseau
  trial: false

image:
  frontend:
    repository: ghcr.io/autoeecoleconnect/frontend
    tag: sha-abc123                   # mis à jour par GitHub Actions
  backend:
    repository: ghcr.io/autoeecoleconnect/backend
    tag: sha-abc123

config:
  logo_url: "https://storage.autoeecoleconnect.app/agence-lyon/logo.png"
  couleur_principale: "#FF5500"
  stripe_enabled: true
  payplug_enabled: false
  alma_enabled: true
  cpf_enabled: false

postgresql:
  storage_size: "10Gi"
  backup_retention: "30d"

resources:
  backend:
    requests: { cpu: "200m", memory: "256Mi" }
    limits:   { cpu: "500m", memory: "512Mi" }
  frontend:
    requests: { cpu: "100m", memory: "128Mi" }
    limits:   { cpu: "250m", memory: "256Mi" }
```

Quand une nouvelle auto-école s'inscrit, le Provisioning Service crée ce fichier via l'API GitHub. ArgoCD le détecte et déploie automatiquement le namespace complet.

---

## 4.5 Flux complet — de l'inscription au namespace opérationnel

```
1. Gérant remplit autoeecoleconnect.app/rejoindre
        │
2. Control Plane valide et crée l'entrée en BDD
        │ (organisation + tenant en statut "provisioning")
        │
3. Provisioning Service génère values.yaml
        │
4. Appel API GitHub → commit dans autoeecoleconnect-infra/tenants/agence-lyon/
        │
5. ArgoCD détecte le nouveau fichier (poll toutes les 3 minutes)
        │
6. ArgoCD applique le Helm chart :
   ├── Crée le namespace agence-lyon
   ├── Déploie CloudNativePG → PostgreSQL démarre
   ├── Liquibase applique les migrations SQL (au démarrage Spring Boot)
   ├── Déploie Spring Boot backend
   ├── Déploie Next.js frontend
   ├── Configure Cilium HTTPRoute → lyon.autoeecoleconnect.app opérationnel
   └── Configure NetworkPolicy → isolation totale des autres tenants
        │
7. ArgoCD notifie le Control Plane (webhook "sync succeeded")
        │
8. Control Plane met à jour le statut tenant = "trial"
        │
9. Resend envoie l'email de bienvenue avec l'URL et les credentials
        │ (délai total : ~2-3 minutes depuis l'inscription)
        ▼
   lyon.autoeecoleconnect.app opérationnel ✅
```

---

## 🎓 Montée en compétence — Monorepo, GitOps et Helm

### Comprendre le GitOps

GitOps est un paradigme où **Git est la source de vérité** pour l'état de l'infrastructure. Si tu veux changer quelque chose en prod, tu fais un commit — pas une commande `kubectl apply` manuelle.

```
Approche classique (à éviter) :
  kubectl apply -f deployment.yaml    ← qui l'a fait ? quand ? pourquoi ?
  helm upgrade tenant ...             ← pas tracé dans Git

Approche GitOps (ArgoCD) :
  git commit → ArgoCD sync            ← historique complet dans Git
  git revert → ArgoCD sync            ← rollback en une commande
```

**Avantages concrets :**
- Audit trail complet de toutes les opérations d'infra
- Rollback = `git revert` + push
- Environnement reproductible : `git clone` + ArgoCD = cluster identique
- Pas d'accès `kubectl` direct en prod pour l'équipe de dev

### Comprendre le Helm Chart par tenant

Helm est un **gestionnaire de packages K8s**. Un chart = un template réutilisable. Pour AutoEcoleConnect, un seul chart `portail-tenant` est instancié autant de fois qu'il y a de tenants, chacun avec son propre `values.yaml`.

```bash
# Installer un tenant manuellement (pour debug)
helm install agence-lyon ./helm/portail-tenant \
  -f autoeecoleconnect-infra/tenants/agence-lyon/values.yaml \
  -n agence-lyon

# Mettre à jour un tenant
helm upgrade agence-lyon ./helm/portail-tenant \
  -f autoeecoleconnect-infra/tenants/agence-lyon/values.yaml

# Voir l'état d'un release Helm
helm status agence-lyon -n agence-lyon
```

**Ressources pour progresser :**
- [Helm docs — Chart Template Guide](https://helm.sh/docs/chart_template_guide/)
- [ArgoCD — ApplicationSet](https://argo-cd.readthedocs.io/en/stable/user-guide/application-set/) (pour déployer N tenants automatiquement depuis un pattern de dossiers)
- [GitOps with ArgoCD](https://codefresh.io/learn/gitops/) (Codefresh — tutoriels pratiques)

**Ce que ça t'apporte sur le CV :**
Concevoir une structure GitOps avec Helm charts paramétrés et provisioning automatique via API GitHub est le cœur du métier de **Platform Engineer**. C'est exactement ce que font les équipes plateforme chez les scale-ups françaises (Qonto, Doctolib, Contentsquare).

---

