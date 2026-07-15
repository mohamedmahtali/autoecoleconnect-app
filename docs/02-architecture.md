---
noteId: "38da9e427f8711f1878859078c773cc2"
tags: []

---

# 2. Architecture Globale

## 2.1 Vue d'ensemble

```
                        INTERNET
                            │
                     ┌──────▼──────┐
                     │  Cloudflare  │  DNS anycast + DDoS + CDN + SSL
                     └──────┬──────┘
                            │ HTTPS
                 ┌──────────▼──────────┐
                 │   Hetzner Bare Metal │
                 │   kubeadm + Cilium   │
                 └──────────┬──────────┘
                            │
          ┌─────────────────┼──────────────────┐
          │                 │                  │
   ┌──────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐
   │  namespace   │  │  namespace   │  │  namespace   │
   │  platform    │  │ agence-lyon  │  │ agence-paris │
   │              │  │              │  │              │
   │ Control Plane│  │ Next.js pod  │  │ Next.js pod  │
   │ Next.js      │  │ Spring Boot  │  │ Spring Boot  │
   │ Spring Boot  │  │ PostgreSQL   │  │ PostgreSQL   │
   │ PostgreSQL   │  │ (CloudNativePG)│ │(CloudNativePG)│
   │              │  │              │  │              │
   │ RabbitMQ     │  │              │  │              │
   │ Resend       │  │              │  │              │
   └──────────────┘  └──────────────┘  └──────────────┘
          │                 │                  │
          └─────────────────▼──────────────────┘
                     Cilium NetworkPolicy
                  (isolation inter-namespaces)
```

---

## 2.2 Les quatre composants principaux

### Composant 1 — Control Plane (`namespace: platform`)

Le cerveau de la plateforme. C'est l'application que les gérants voient sur `autoeecoleconnect.app`.

```
autoeecoleconnect.app
├── /rejoindre          → formulaire inscription auto-école
├── /dashboard          → tableau de bord gérant (CA consolidé)
├── /admin              → super-admin (liste tenants, suspension, métriques)
└── /api/internal/
    ├── /provision      → crée un namespace K8s + déploie les pods
    ├── /suspend        → stoppe les pods d'un tenant (trial expiré)
    └── /deprovision    → supprime le namespace définitivement
```

**Stack** : Next.js (frontend) + Spring Boot (API interne) + PostgreSQL dédié

---

### Composant 2 — Provisioning Service (dans `namespace: platform`)

Le service qui orchestre la création d'un nouveau tenant via **GitOps**.

```
Flux de provisioning :

1. Gérant remplit le formulaire sur autoeecoleconnect.app
2. Spring Boot (Control Plane) reçoit la demande
3. Provisioning Service appelle l'API GitHub
   → crée autoeecoleconnect-infra/tenants/agence-lyon/values.yaml
4. ArgoCD détecte le nouveau fichier (sync automatique)
5. ArgoCD applique le Helm chart → namespace créé + pods déployés
6. CloudNativePG démarre PostgreSQL → Liquibase applique les migrations
7. ArgoCD notifie le Control Plane (webhook "sync succeeded")
   → statut tenant = "trial"
8. Resend envoie l'email de bienvenue avec l'URL
```

Toutes les opérations d'infrastructure passent par **Git** — traçabilité totale.

---

### Composant 3 — Portail Tenant (`namespace: agence-xxx`)

L'application que les auto-écoles utilisent au quotidien. Chaque tenant a sa propre instance.

```
agence-lyon.autoeecoleconnect.app
├── Pod Next.js         → interface directeur, moniteurs, élèves
├── Pod Spring Boot     → API REST métier (réservations, planning, paiements)
└── Pod PostgreSQL      → données exclusives de cette auto-école
    (CloudNativePG)       backup nightly → Hetzner Object Storage S3
```

Chaque pod ne connaît qu'**une seule auto-école**. Pas de filtre `agence_id`, pas de multi-tenancy dans le code — l'isolation est au niveau K8s.

---

### Composant 4 — Infrastructure partagée (`namespace: platform`)

Services utilisés par tous les tenants mais gérés centralement :

```
RabbitMQ        → événements inter-composants (provisioning, métriques, emails)
Resend          → emails transactionnels (bienvenue, rappels, alertes)
Prometheus      → métriques de tous les namespaces
Loki            → logs de tous les namespaces
Grafana         → dashboards par tenant + alertes
Velero          → snapshots namespaces complets (backup disaster recovery)
```

---

## 2.3 Flux de communication

```
Tenant Lyon publie des événements RabbitMQ :
  ReservationCreated  → Resend envoie email confirmation au client
  ReservationPaid     → Control Plane agrège le CA de l'organisation
  TrialExpiringSoon   → Resend envoie email de rappel au gérant
  PaymentFailed       → alerte Grafana + email gérant

Control Plane publie :
  TenantProvisionRequested  → Provisioning Service crée le namespace
  TenantSuspendRequested    → Provisioning Service stoppe les pods
  TenantDeleteRequested     → Provisioning Service supprime le namespace
```

---

## 2.4 Isolation réseau (Cilium NetworkPolicy)

```
namespace agence-lyon   ──✂──  namespace agence-paris
      │                              │
      │  ❌ aucune communication     │
      │     possible entre tenants   │
      │                              │
      ▼                              ▼
 namespace platform           namespace platform
      │                              │
      ✅ RabbitMQ (events only)      ✅ RabbitMQ (events only)
      ✅ Prometheus (metrics scrape) ✅ Prometheus (metrics scrape)
```

Un tenant ne peut jamais atteindre les pods d'un autre tenant. Cilium enforce cette règle au niveau du kernel Linux (eBPF) — pas une règle logicielle contournable.

---

## 🎓 Montée en compétence — Architecture K8s

### Comprendre les namespaces K8s

Un namespace K8s est une **partition logique** du cluster. Pense-y comme des dossiers dans un système de fichiers : les fichiers dans `/home/alice/` sont isolés de `/home/bob/` même s'ils sont sur le même disque.

```bash
# Voir tous les namespaces
kubectl get namespaces

# Voir les pods d'un tenant spécifique
kubectl get pods -n agence-lyon

# Voir les ressources consommées par namespace
kubectl top pods -n agence-lyon
```

**Ordre d'apprentissage recommandé :**

1. **Kubernetes basics** (1-2 semaines)
   - Pods, Deployments, Services, ConfigMaps, Secrets
   - `kubectl` : get, describe, logs, exec, apply, delete
   - Ressource : [Kubernetes.io - Learn K8s](https://kubernetes.io/docs/tutorials/)

2. **Networking K8s** (1 semaine)
   - Comment les pods se parlent (ClusterIP, DNS interne)
   - NetworkPolicy : autoriser/bloquer le trafic entre namespaces
   - Ressource : [Cilium Network Policy Editor](https://editor.cilium.io/)

3. **Helm** (3-4 jours)
   - Templating de manifests K8s
   - `helm install`, `helm upgrade`, `values.yaml`
   - Ressource : [Helm.sh Quickstart](https://helm.sh/docs/intro/quickstart/)

4. **ArgoCD** (3-4 jours)
   - GitOps : ArgoCD surveille un repo Git et sync l'état K8s
   - `Application` CRD, auto-sync, rollback
   - Ressource : [ArgoCD Getting Started](https://argo-cd.readthedocs.io/en/stable/getting_started/)

### SaaS multi-tenant — comprendre les modèles d'isolation

Avant de coder quoi que ce soit, il faut maîtriser les trois modèles d'isolation multi-tenant et savoir lequel choisir selon le contexte :

| Modèle | Isolation | Complexité | Choix AutoEcoleConnect |
|---|---|---|---|
| Row-level (agence_id partout) | Logicielle | Faible | ❌ Risque fuite si bug |
| Schema-per-tenant | PostgreSQL | Moyenne | ❌ Pas assez isolé |
| **Namespace-per-tenant** | **K8s + BDD** | **Haute** | **✅ Choix retenu** |

**Pourquoi namespace-per-tenant ?**
Chaque auto-école a son propre Spring Boot, son propre PostgreSQL, ses propres ressources K8s. Un bug dans le code n'expose jamais les données d'un autre tenant. L'isolation est physique, pas logicielle.

**Ressources pour approfondir :**
- [The SaaS CTO Security Guide](https://www.latacora.com/blog/2020/03/12/the-saas-cto/) — comprendre les enjeux sécurité d'un SaaS
- [Multi-tenancy patterns](https://docs.microsoft.com/en-us/azure/architecture/guide/multitenant/overview) (Microsoft Architecture Center) — référence complète
- Livre : *Building Multi-Tenant SaaS Architectures* (O'Reilly)

**Ce que ça t'apporte sur le CV :**
Savoir expliquer les trade-offs entre les modèles d'isolation multi-tenant en entretien est un signal fort de maturité architecturale — la plupart des candidats ne connaissent que le row-level. Et maîtriser l'architecture namespace-per-tenant avec GitOps (ArgoCD), l'isolation réseau Cilium eBPF et le provisioning automatisé démontre une vraie expertise plateforme.

---

