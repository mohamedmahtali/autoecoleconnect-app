---
noteId: "38da77307f8711f1878859078c773cc2"
tags: []

---

# 1. Vision & Ambition

## 1.1 Le point de départ — PermisOne

**PermisOne** est le POC existant : une application de gestion d'auto-école développée avec **Next.js 15 + Supabase + Stripe**, destinée à **une seule auto-école**. Elle permet de gérer :

- Ses clients (élèves) et leur dossier
- Ses moniteurs et leurs disponibilités
- Son parc de véhicules
- Ses forfaits de conduite
- Ses réservations et séances
- Ses paiements (Stripe ou manuel)
- Ses documents (CIN, permis, livrets)

C'est un MVP fonctionnel, mais mono-tenant : conçu pour une seule auto-école, pas pour être partagé entre plusieurs.

**AutoEcoleConnect** part de ce périmètre fonctionnel pour construire une **plateforme SaaS multi-tenant** repensée de zéro sur une nouvelle stack (Java 21 + Spring Boot + Kubernetes — voir [3. Stack Technique Complète](03-stack-technique.md)), où chaque auto-école crée et gère son propre espace isolé. Ce n'est pas une évolution du code de PermisOne : c'est un nouveau produit qui en reprend l'idée et les fonctionnalités.

---

## 1.2 La vision cible

> **Chaque auto-école qui s'inscrit sur AutoEcoleConnect obtient automatiquement son propre espace applicatif complet — frontend, backend, base de données — totalement isolé des autres.**

```
autoeecoleconnect.app                    ← site vitrine + inscription auto-école
    │
    ▼ inscription + trial 30j
lyon.autoeecoleconnect.app               ← espace Auto-École Lyon (Next.js + Spring Boot + PostgreSQL)
paris.autoeecoleconnect.app              ← espace Auto-École Paris (totalement isolé)
bordeaux.autoeecoleconnect.app           ← espace Auto-École Bordeaux (totalement isolé)
    │
    ▼ gérant multi-agences
groupe-martin.autoeecoleconnect.app      ← dashboard consolidé CA, élèves, réservations
```

Chaque espace est un **namespace Kubernetes indépendant** avec ses propres pods et sa propre base de données. L'isolation est totale — un bug ou une surcharge chez un tenant n'affecte pas les autres.

---

## 1.3 Les deux ambitions du projet

**Ambition produit** — construire un vrai SaaS rentable sur le marché français des auto-écoles :
- ~12 000 auto-écoles en France
- Marché peu digitalisé, beaucoup de logiciels vieillissants
- Argument différenciateur : données hébergées en Europe, conformité RGPD native
- Modèle freemium avec trial 30 jours sans carte bancaire

**Ambition DevOps** — construire une infrastructure moderne de A à Z :
- Kubernetes vanilla (kubeadm) sur bare metal Hetzner
- Stack GitOps complète : Terraform + Ansible + ArgoCD
- Observabilité : Prometheus + Loki + Grafana
- Sécurité : Cilium NetworkPolicy + Sealed Secrets + cert-manager
- Ce projet est un **portfolio DevOps complet** démontrant la maîtrise de l'écosystème cloud-native

---

## 1.4 Modèle d'organisation

Un gérant peut posséder plusieurs auto-écoles. La plateforme supporte deux niveaux :

```
Organisation "Groupe Martin"          ← un gérant, une facture, un login
├── Auto-École Lyon                   ← namespace K8s isolé
├── Auto-École Paris                  ← namespace K8s isolé
└── Auto-École Bordeaux               ← namespace K8s isolé
    │
    └── Dashboard consolidé
        CA total, élèves total, alertes — agrégé via événements RabbitMQ
```

Un gérant avec une seule auto-école ne perçoit pas la couche organisation — elle est transparente jusqu'à ce qu'il ajoute une deuxième agence.

---

## 1.5 Plans tarifaires

| Plan | Prix | Auto-écoles | Modules |
|---|---|---|---|
| **Solo** | 29€/mois | 1 | Standard (paiement manuel, planning, documents) |
| **Pro** | 59€/mois | 1 | Tous (Stripe, PayPlug, Alma, CPF, statistiques avancées) |
| **Groupe** | 99€/mois | jusqu'à 5 | Tous modules + dashboard consolidé multi-agences |
| **Réseau** | sur devis | illimitées | Franchises, SLA garanti, support dédié |

Toutes les formules démarrent avec un **essai gratuit de 30 jours**, sans carte bancaire.

```
J+0   : inscription → namespace K8s créé automatiquement → email d'accès
J+25  : email de rappel "5 jours restants"
J+30  : sans paiement → namespace suspendu (pods stoppés, données conservées)
J+60  : sans paiement → namespace supprimé définitivement
```

---

## 1.6 Démarche incrémentale

L'architecture cible décrite dans ce document ne se construit pas d'un bloc. La roadmap introduit la complexité au moment où elle devient nécessaire :

| Phase | Objectif | Déclencheur |
|---|---|---|
| **0 — Fondations** | Backend Spring Boot + frontend Next.js dockerisés, docker-compose local | — |
| **1 — Premier déploiement** | Cluster kubeadm + Cilium sur Hetzner, 1er tenant déployé manuellement (Helm) | App fonctionnelle en local |
| **2 — GitOps** | ArgoCD + ApplicationSet, monitoring PLG, 2-3 tenants provisionnés à la main | 1er tenant stable en prod |
| **3 — Provisioning automatique** | Control Plane + inscription self-service + trial + billing Stripe | 3+ tenants, onboarding manuel trop lent |
| **4 — Industrialisation** | KEDA, Velero, dashboards par tenant, alerting complet | 10+ tenants payants |

Chaque phase produit quelque chose de démontrable — en entretien comme auprès des premiers clients.

---

