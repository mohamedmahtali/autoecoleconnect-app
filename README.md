---
noteId: "521e44707e1e11f1a0dfb35e1fd655c4"
tags: []

---

# AutoEcoleConnect — Plateforme SaaS multi-tenant pour auto-écoles

![Statut](https://img.shields.io/badge/statut-phase%200%20en%20cours-blue)
![Kubernetes](https://img.shields.io/badge/kubernetes-kubeadm%20%2B%20cilium-326CE5?logo=kubernetes&logoColor=white)
![Backend](https://img.shields.io/badge/backend-Java%2021%20·%20Spring%20Boot%203-6DB33F?logo=springboot&logoColor=white)
![Frontend](https://img.shields.io/badge/frontend-Next.js%2015-black?logo=nextdotjs&logoColor=white)

## Statut du projet

- **Point de départ** : **PermisOne**, un POC fonctionnel pour une seule auto-école (Next.js + Supabase + Stripe), développé en dehors de ce dépôt.
- **Objectif** : reconstruire ce périmètre fonctionnel en un nouveau produit, **AutoEcoleConnect**, plateforme SaaS multi-tenant sur une stack Spring Boot + Next.js + Kubernetes (voir [1. Vision & Ambition](docs/01-vision.md)).
- **Phase actuelle** : **Phase 0 — Fondations**, en cours (voir la démarche incrémentale en [1.6](docs/01-vision.md#16-démarche-incrémentale)). Le squelette du monorepo est posé : `backend/` (Spring Boot 3 / Java 21), `frontend/` (Next.js 15) et `docker-compose.yml` pour le dev local.

## Documentation

| # | Document | Contenu |
|---|---|---|
| 1 | [Vision & Ambition](docs/01-vision.md) | Ce qu'est le projet, la vision cible, le modèle d'organisation, les plans tarifaires, la roadmap par phases |
| 2 | [Architecture Globale](docs/02-architecture.md) | Vue d'ensemble, les 4 composants principaux, flux de communication, isolation réseau |
| 3 | [Stack Technique Complète](docs/03-stack-technique.md) | Frontend, backend, infra K8s, GitOps, monitoring — avec justifications |
| 4 | [Structure des Repos](docs/04-structure-repos.md) | Arborescence des deux repos (app + infra GitOps), exemple de `values.yaml` |
| 5 | [Modèle Business](docs/05-modele-business.md) | Facturation B2C/B2B, plans tarifaires détaillés, cycle de vie d'un abonnement, coûts & break-even |
| 6 | [Modèle de Données](docs/06-modele-donnees.md) | Schémas BDD Control Plane et BDD Tenant, entités JPA |
| 7 | [Module Paiement](docs/07-module-paiement.md) | Stripe, PayPlug, Alma, méthodes manuelles, webhooks |
| 8 | [Infrastructure Kubernetes](docs/08-infrastructure-k8s.md) | Cluster kubeadm, Cilium, cert-manager, auto-scaling, backups |
| 9 | [Cycle de vie d'un tenant](docs/09-cycle-vie-tenant.md) | Provisioning, trial, suspension, suppression |
| 10 | [CI/CD & GitOps](docs/10-cicd-gitops.md) | Pipeline GitHub Actions, ArgoCD, Dockerfiles |
| 11 | [Monitoring — Stack PLG](docs/11-monitoring.md) | Prometheus, Loki, Grafana, alertes |
| 12 | [Sécurité](docs/12-securite.md) | Sealed Secrets, NetworkPolicy, RBAC, RGPD |
| 13 | [Analytics — KPIs du directeur](docs/13-analytics.md) | Tableau de bord statistiques (saisonnalité, taux d'abandon, occupation) |
| — | [Roadmap de montée en compétence](docs/ROADMAP.md) | Récapitulatif des technologies à maîtriser et certifications ciblées |

Chaque document contient aussi ses propres sections **🎓 Montée en compétence** (parcours d'apprentissage lié au sujet traité).

---

## Démarrage rapide — dev local

```bash
# Prérequis : Docker + Docker Compose (Java 21 et Node 22 uniquement pour le dev hors conteneur)

# Lance PostgreSQL + RabbitMQ + backend + frontend
docker compose up -d --build

# Frontend  → http://localhost:3000
# Backend   → http://localhost:8080  (Swagger UI : /swagger-ui.html)
# RabbitMQ  → http://localhost:15672 (guest/guest)
```

L'API est protégée par JWT. Un compte directeur bootstrap est créé au premier
démarrage (`admin@autoecoleconnect.local` / `changez-moi-en-production`, surchargeable
via `ADMIN_EMAIL` / `ADMIN_PASSWORD`) :

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@autoecoleconnect.local","motDePasse":"changez-moi-en-production"}'
# → {"token":"...", ...} puis Authorization: Bearer <token>
# (bouton « Authorize » dans Swagger UI)
```

Pour des surcharges locales (ports, volumes), copier `docker-compose.override.yml.example` en `docker-compose.override.yml` (non versionné).

Le `docker-compose.yml` reproduit en local la stack d'un tenant (sans K8s) : Next.js, Spring Boot, PostgreSQL et RabbitMQ. Les migrations Liquibase s'appliquent automatiquement au démarrage du backend.
