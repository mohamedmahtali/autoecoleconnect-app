---
noteId: "38da9e417f8711f1878859078c773cc2"
tags: []

---

# 3. Stack Technique Complète

## 3.1 Vue d'ensemble par couche

```
┌─────────────────────────────────────────────────────────┐
│  FRONTEND           Next.js 15 (TypeScript)             │
│                     App Router, REST client, Tailwind   │
├─────────────────────────────────────────────────────────┤
│  BACKEND            Java 21 LTS + Spring Boot 3.x       │
│                     REST API, Spring Security, AMQP     │
├─────────────────────────────────────────────────────────┤
│  BASE DE DONNÉES    PostgreSQL via CloudNativePG        │
│                     Migrations : Liquibase              │
├─────────────────────────────────────────────────────────┤
│  MESSAGE QUEUE      RabbitMQ (architecture Kafka-ready) │
├─────────────────────────────────────────────────────────┤
│  STORAGE            Hetzner Object Storage (S3-compat.) │
├─────────────────────────────────────────────────────────┤
│  EMAILS             Resend                              │
├─────────────────────────────────────────────────────────┤
│  KUBERNETES         kubeadm (vanilla) sur Hetzner       │
│                     Cilium (CNI + Gateway API)          │
│                     CloudNativePG, KEDA, HPA            │
├─────────────────────────────────────────────────────────┤
│  GITOPS             GitHub Actions → GHCR → ArgoCD      │
├─────────────────────────────────────────────────────────┤
│  IaC                Terraform (Hetzner) + Ansible (K8s) │
├─────────────────────────────────────────────────────────┤
│  MONITORING         Prometheus + Loki + Grafana (PLG)   │
├─────────────────────────────────────────────────────────┤
│  SÉCURITÉ           Cilium NetworkPolicy + Sealed Secrets│
│                     cert-manager + Let's Encrypt        │
│                     Cloudflare (DNS + DDoS + CDN)       │
├─────────────────────────────────────────────────────────┤
│  BACKUP             CloudNativePG Backup + Velero       │
└─────────────────────────────────────────────────────────┘
```

---

## 3.2 Application

### Frontend — Next.js 15 (TypeScript)

| Aspect | Détail |
|---|---|
| Framework | Next.js 15 avec App Router |
| Langage | TypeScript |
| Style | Tailwind CSS |
| API client | Fetch natif + types générés depuis OpenAPI (Spring Boot) |
| Auth | JWT stocké en cookie HttpOnly (émis par Spring Security) |
| Containerisation | `output: 'standalone'` → image Docker ~150MB |

**Pourquoi Next.js ?** Rendu hybride (SSR/SSG/CSR), routing basé sur les fichiers, excellent DX TypeScript. Standard actuel pour les frontends React en production.

---

### Backend — Java 21 LTS + Spring Boot 3.x

| Aspect | Détail |
|---|---|
| Langage | Java 21 LTS (support jusqu'en 2031) |
| Framework | Spring Boot 3.x |
| API | REST + OpenAPI/Swagger (springdoc-openapi) |
| Auth | Spring Security + JWT (stateless) |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Liquibase |
| Messaging | Spring AMQP (RabbitMQ) |
| Tests | JUnit 5 + Mockito + Testcontainers |

**Pourquoi Java 21 ?** Support LTS jusqu'en 2031. Chaque pod Spring Boot sert une seule auto-école — la charge par instance est faible. Les virtual threads sont disponibles mais non nécessaires à ce stade.

**Pourquoi Spring Boot ?** Écosystème enterprise mature, Spring Security robuste pour le JWT, Spring Data JPA productif, intégration AMQP native pour RabbitMQ. Fort signal CV pour les postes backend Java.

---

### Communication Frontend ↔ Backend — REST + OpenAPI

```
Spring Boot expose /v3/api-docs (spec OpenAPI auto-générée)
        ↓
openapi-typescript génère les types TypeScript
        ↓
Next.js consomme des types 100% synchronisés avec le backend

# Commande de génération (à lancer après chaque changement d'API)
npx openapi-typescript http://backend:8080/v3/api-docs -o src/types/api.ts
```

---

## 3.3 Infrastructure K8s

### Cluster — kubeadm sur Hetzner Bare Metal

| Composant | Choix | Justification |
|---|---|---|
| Serveur | Hetzner AX41 (64GB RAM, NVMe) | Meilleur rapport perf/prix/support Europe |
| K8s distro | kubeadm (vanilla officiel) | Apprentissage des internals, contrôle total |
| CNI réseau | Cilium (eBPF) | NetworkPolicy avancées + Gateway API intégré |
| Ingress | Cilium Gateway API | Zéro pod supplémentaire, futur standard K8s |
| Auto-scaling | KEDA + HPA | Scaling event-driven (RabbitMQ) + CPU/RAM |

**Hetzner AX41 — ce que tu as pour ~55€/mois :**
```
CPU    : AMD Ryzen 5 3600 (6 cores / 12 threads)
RAM    : 64 GB DDR4
Disque : 2 × 512 GB NVMe SSD (RAID 1)
Réseau : 1 Gbps
Capacité estimée : 20-25 namespaces tenants + plateforme complète
```

**Pourquoi kubeadm et pas k3s ?**
kubeadm installe chaque composant K8s séparément. Tu vois et comprends chaque pièce (etcd, API server, scheduler). k3s les emballe dans un binaire — efficace mais opaque. Pour un portfolio DevOps, kubeadm démontre une vraie maîtrise des internals.

---

### Base de données — CloudNativePG

Un opérateur K8s qui gère PostgreSQL comme une ressource native :

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: postgres-agence-lyon
  namespace: agence-lyon
spec:
  instances: 1
  storage:
    size: 10Gi
  backup:
    barmanObjectStore:
      destinationPath: s3://autoeecoleconnect-backups/agence-lyon
      schedule: "0 2 * * *"    # backup nightly à 2h00
```

Chaque tenant a son propre cluster PostgreSQL. Backup automatique vers Hetzner Object Storage avec PITR (Point-In-Time Recovery) — restauration à la minute exacte avant un incident.

---

### Message Queue — RabbitMQ (architecture Kafka-ready)

RabbitMQ est suffisant pour le volume actuel (< 100 événements/minute même à 500 auto-écoles). Les événements sont typés et découplés — migrer vers Kafka en Phase 3 ne change que le broker, pas le code.

```
Événements système :
  TenantProvisioningRequested / Completed / Failed
  TenantSuspendRequested / TenantDeleteRequested

Événements métier (publiés par chaque tenant) :
  ReservationCreated / Paid / Cancelled
  PaymentSucceeded / Failed
  TrialExpiringSoon / Expired
  OrgMetricsUpdated   ← agrégation CA pour dashboard gérant
```

---

## 3.4 GitOps & CI/CD

```
git push origin main
        │
        ▼
GitHub Actions
  ├── Tests (JUnit + Jest)
  ├── Build image Next.js   → ghcr.io/autoeecoleconnect/frontend:sha-abc123
  ├── Build image Spring Boot→ ghcr.io/autoeecoleconnect/backend:sha-abc123
  └── Commit dans autoeecoleconnect-infra/
      └── values.yaml: image.tag: sha-abc123
              │
              ▼ (détection automatique par ArgoCD)
        ArgoCD sync → rolling update sur K8s
```

**autoeecoleconnect/** (monorepo app) et **autoeecoleconnect-infra/** (GitOps) sont deux repos séparés.
ArgoCD surveille uniquement `autoeecoleconnect-infra/` — le code source n'est jamais accessible à ArgoCD.

---

## 3.5 Monitoring — Stack PLG

| Outil | Rôle |
|---|---|
| Prometheus | Collecte métriques (pods, JVM, PostgreSQL, RabbitMQ) |
| Loki | Agrège logs de tous les namespaces |
| Grafana | Dashboards par tenant + alertes |
| AlertManager | Notifications (email, Slack) si pod down ou disque plein |

Dashboards configurés par tenant : CPU/RAM, réservations actives, taux d'erreur API, latence PostgreSQL, espace disque.

---

## 🎓 Montée en compétence — La stack dans son ensemble

### Ordre d'apprentissage recommandé (semaine par semaine)

**Bloc 1 — Java + Spring Boot** (si tu pars de zéro en Java)
```
Semaines 1-2 : Java 21 (types, collections, streams, records, optionals)
Semaines 3-4 : Spring Boot REST (controllers, services, repositories, JPA)
Semaine 5    : Spring Security (JWT stateless, filtres, autorisation)
Semaine 6    : Liquibase (migrations versionnées, rollback)
Ressource    : "Spring Boot 3 en action" (Manning) + Baeldung.com
```

**Bloc 2 — Kubernetes** (parallèle au Bloc 1)
```
Semaine 1    : kubectl + Pods / Deployments / Services / ConfigMaps
Semaine 2    : kubeadm install sur VM locale (Proxmox = idéal)
Semaine 3    : Helm (créer un chart from scratch pour Spring Boot)
Semaine 4    : ArgoCD (GitOps, Application CRD, auto-sync, rollback)
Ressource    : kubernetes.io/docs + killer.sh (simulateur CKA)
```

**Bloc 3 — Réseau & Sécurité K8s**
```
Semaine 1    : Cilium install + NetworkPolicy (autoriser/bloquer namespaces)
Semaine 2    : Cilium Gateway API (routing HTTP par tenant)
Semaine 3    : cert-manager + Let's Encrypt (wildcard TLS automatique)
Semaine 4    : Sealed Secrets (secrets chiffrés dans Git)
Ressource    : isovalent.com/labs (labs Cilium gratuits en ligne)
```

**Bloc 4 — Observabilité**
```
Semaine 1    : Prometheus (PromQL, scraping, alerting rules)
Semaine 2    : Grafana (dashboards, variables, alertes)
Semaine 3    : Loki + Promtail (log aggregation, LogQL)
Ressource    : grafana.com/tutorials
```

**Bloc 5 — IaC**
```
Semaine 1    : Terraform basics (providers, resources, plan, apply, state)
Semaine 2    : Terraform Hetzner (provisionner le serveur bare metal)
Semaine 3    : Ansible (playbooks, rôles, handlers)
Semaine 4    : Ansible + kubeadm (bootstrap complet du cluster en playbook)
Ressource    : HashiCorp Learn + "Ansible for DevOps" (Jeff Geerling — livre gratuit)
```

### Certifications recommandées dans l'ordre

| Certification | Valeur marché | Durée préparation | Coût |
|---|---|---|---|
| **CKA** (Certified K8s Administrator) | Très haute | 2-3 mois | $395 |
| **CKAD** (Certified K8s App Developer) | Haute | 1-2 mois | $395 |
| **Terraform Associate** | Bonne | 3-4 semaines | $70 |

Commence par **CKA** — il valide exactement ce que tu construis ici. C'est la certification la plus reconnue en Platform Engineering / DevOps.

**Ce que cette stack t'apporte sur le CV :**
Opérer kubeadm + Cilium + CloudNativePG + ArgoCD sur bare metal en production est rare. La majorité des ingénieurs DevOps travaillent sur Managed K8s (EKS, GKE) où la complexité est cachée. Comprendre les internals (etcd, kubeadm bootstrap, Cilium eBPF, CNPG opérateur) se voit immédiatement en entretien.

---

