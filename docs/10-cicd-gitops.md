---
noteId: "38dac5547f8711f1878859078c773cc2"
tags: []

---

# 10. CI/CD & GitOps

## 10.1 Vue d'ensemble du pipeline

```
┌─────────────────────────────────────────────────────────────────────┐
│                  Pipeline complet AutoEcoleConnect                        │
└─────────────────────────────────────────────────────────────────────┘

  Developer                 GitHub Actions                ArgoCD
  ─────────                 ──────────────                ──────
  git push ──────────────► [CI — Test + Lint]
  feature/xyz              [Build Docker image]
                           [Push GHCR]            ───────►[Detect new tag]
                           [Update image tag               [Sync cluster]
                            in autoeecoleconnect-infra/]  ───────►[Apply manifests]
                                                           [Health check]
                                                           [Webhook → Slack]

  Deux repos :
  ┌───────────────────────────┐     ┌───────────────────────────────┐
  │  autoeecoleconnect/               │     │  autoeecoleconnect-infra/             │
  │  (code applicatif)        │     │  (état désiré du cluster)     │
  │                           │     │                               │
  │  backend/                 │     │  helm/portail-tenant/         │
  │  frontend/                │     │  argocd/                      │
  │  control-plane/           │     │  tenants/                     │
  │                           │     │    agence-lyon/values.yaml    │
  │  → GitHub Actions CI/CD   │     │    agence-paris/values.yaml   │
  │  → push image tag GHCR    │     │                               │
  └───────────────────────────┘     │                               │
                                    │  → ArgoCD sync automatique    │
                                    └───────────────────────────────┘
```

---

## 10.2 Stratégie de branches

```
main          ──────●──────────────────●──────────────────● (production)
                    │                  ▲                  ▲
                    │   merge PR       │  merge PR        │
feature/xxx   ──────●──────────►       │                  │
                                       │                  │
feature/yyy   ─────────────────────────●──────────►       │
                                                          │
hotfix/zzz    ────────────────────────────────────────────●──────►
```

| Branche | Déploiement | Tests |
|---|---|---|
| `feature/*` | Preview optionnel | CI obligatoire (lint + tests) |
| `main` | Production auto via ArgoCD | CI + CD complet |
| `hotfix/*` | Production direct (merge → main) | CI obligatoire |

---

## 10.3 GitHub Actions — CI (test + build)

Fichier : `.github/workflows/ci.yml` dans le monorepo `autoeecoleconnect/`

```yaml
name: CI

on:
  push:
    branches: ["**"]
  pull_request:
    branches: [main]

jobs:
  # ─── Backend Spring Boot ───────────────────────────────────────────
  backend-ci:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: maven

      - name: Lint + Tests
        run: mvn verify --no-transfer-progress

      - name: Upload coverage
        uses: codecov/codecov-action@v4
        with:
          files: target/site/jacoco/jacoco.xml

  # ─── Frontend Next.js ──────────────────────────────────────────────
  frontend-ci:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node 22
        uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: "npm"

      - name: Install deps
        run: npm ci

      - name: Type check
        run: npm run type-check

      - name: Lint
        run: npm run lint

      - name: Tests
        run: npm test -- --passWithNoTests

      - name: Build
        run: npm run build
```

---

## 10.4 GitHub Actions — CD (build image + push GHCR)

Fichier : `.github/workflows/cd.yml` — ne se déclenche que sur `main`

```yaml
name: CD

on:
  push:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_PREFIX: ghcr.io/autoeecoleconnect

jobs:
  # ─── Build & push backend ─────────────────────────────────────────
  build-backend:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    outputs:
      image-tag: ${{ steps.meta.outputs.version }}

    steps:
      - uses: actions/checkout@v4

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Docker metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.IMAGE_PREFIX }}/backend
          tags: |
            type=sha,prefix=,suffix=,format=short
            type=raw,value=latest,enable=${{ github.ref == 'refs/heads/main' }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: backend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # ─── Build & push frontend ────────────────────────────────────────
  build-frontend:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    outputs:
      image-tag: ${{ steps.meta.outputs.version }}

    steps:
      - uses: actions/checkout@v4

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Docker metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.IMAGE_PREFIX }}/frontend
          tags: |
            type=sha,prefix=,suffix=,format=short
            type=raw,value=latest

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: frontend
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # ─── Mise à jour du repo GitOps ───────────────────────────────────
  update-gitops:
    runs-on: ubuntu-latest
    needs: [build-backend, build-frontend]

    steps:
      - name: Checkout autoeecoleconnect-infra
        uses: actions/checkout@v4
        with:
          repository: autoeecoleconnect/autoeecoleconnect-infra
          token: ${{ secrets.GITOPS_TOKEN }}    # PAT avec droit write sur autoeecoleconnect-infra
          path: infra

      - name: Update image tags
        run: |
          cd infra
          # Mise à jour des tags dans le values.yaml de base du chart (hérité par tous les tenants)
          # yq cible un chemin YAML précis — un sed sur "tag:" toucherait les deux images
          yq -i '.image.backend.tag  = "${{ needs.build-backend.outputs.image-tag }}"' \
            helm/portail-tenant/values.yaml
          yq -i '.image.frontend.tag = "${{ needs.build-frontend.outputs.image-tag }}"' \
            helm/portail-tenant/values.yaml

      - name: Commit and push
        run: |
          cd infra
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add helm/portail-tenant/values.yaml
          git commit -m "ci: bump images to ${{ github.sha }}"
          git push
```

ArgoCD surveille `autoeecoleconnect-infra/` et applique le changement dans le cluster automatiquement après ce push.

---

## 10.5 ArgoCD — ApplicationSet multi-tenant

L'`ApplicationSet` est la pièce centrale du GitOps : il génère une `Application` ArgoCD par répertoire dans `tenants/`.

```yaml
# autoeecoleconnect-infra/argocd/applicationset-tenants.yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: tenants
  namespace: argocd
spec:
  # Générateur : une Application par sous-dossier de tenants/
  generators:
    - git:
        repoURL: https://github.com/autoeecoleconnect/autoeecoleconnect-infra.git
        revision: main
        directories:
          - path: "tenants/*"

  template:
    metadata:
      name: "tenant-{{path.basename}}"    # ex: tenant-agence-lyon
    spec:
      project: tenants

      # Deux sources : le chart Helm commun + le values.yaml du tenant
      # (une source ne peut pas avoir à la fois path et chart — pattern multi-source ArgoCD 2.6+)
      sources:
        - repoURL: https://github.com/autoeecoleconnect/autoeecoleconnect-infra.git
          targetRevision: main
          path: helm/portail-tenant       # chart commun à tous les tenants
          helm:
            valueFiles:
              - $values/tenants/{{path.basename}}/values.yaml
        - repoURL: https://github.com/autoeecoleconnect/autoeecoleconnect-infra.git
          targetRevision: main
          ref: values                     # référence "$values" utilisée ci-dessus

      destination:
        server: https://kubernetes.default.svc
        namespace: "{{path.basename}}"    # agence-lyon

      syncPolicy:
        automated:
          prune: true       # supprime les ressources retirées du repo
          selfHeal: true    # re-applique si quelqu'un modifie le cluster manuellement
        syncOptions:
          - CreateNamespace=true
```

**Ce que ça signifie concrètement :**
- Ajouter un tenant = créer `tenants/agence-bordeaux/values.yaml` dans le repo → ArgoCD détecte → namespace créé automatiquement
- Supprimer un tenant = supprimer le dossier → ArgoCD `prune: true` → namespace supprimé
- Modifier un tenant = modifier `values.yaml` → ArgoCD resync → rolling update

---

## 10.6 ArgoCD — Application Control Plane

Le Control Plane (l'app plateforme elle-même) est aussi géré par ArgoCD :

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: control-plane
  namespace: argocd
spec:
  project: platform

  source:
    repoURL: https://github.com/autoeecoleconnect/autoeecoleconnect-infra.git
    targetRevision: main
    path: helm/control-plane
    helm:
      valueFiles:
        - values.yaml

  destination:
    server: https://kubernetes.default.svc
    namespace: platform

  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

---

## 10.7 Dockerfiles

### Backend Spring Boot

```dockerfile
# backend/Dockerfile
# L'image temurin ne contient pas Maven — on utilise l'image maven officielle pour le build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
# Copier le pom seul d'abord : les dépendances sont mises en cache par Docker
COPY pom.xml .
RUN mvn dependency:go-offline --no-transfer-progress
COPY src ./src
RUN mvn package -DskipTests --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app
# Utilisateur non-root (bonne pratique sécurité)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Next.js

```dockerfile
# frontend/Dockerfile
FROM node:22-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci

FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# Image finale légère — standalone output de Next.js
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

RUN addgroup -S nextjs && adduser -S nextjs -G nextjs
USER nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nextjs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nextjs /app/.next/static ./.next/static

EXPOSE 3000
CMD ["node", "server.js"]
```

`output: 'standalone'` dans `next.config.ts` est obligatoire pour l'image légère (~150 MB vs ~800 MB sans).

---

## 10.8 Secrets CI/CD

Les secrets GitHub Actions nécessaires :

| Secret | Valeur | Usage |
|---|---|---|
| `GITHUB_TOKEN` | Auto-généré par GitHub | Push sur GHCR (packages) |
| `GITOPS_TOKEN` | PAT GitHub (write sur autoeecoleconnect-infra) | Mise à jour des image tags |

Les secrets applicatifs (Stripe, DB...) ne transitent **jamais** par GitHub Actions — ils sont dans Sealed Secrets directement dans `autoeecoleconnect-infra/`.

---

## 🎓 Montée en compétence — CI/CD & GitOps

### GitOps vs CI/CD classique

```
CI/CD classique (push-based) :
  Pipeline → kubectl apply → cluster
  Problème : le pipeline a un accès direct au cluster (credentials risqués)
  Problème : si le pipeline tombe, l'état du cluster est inconnu

GitOps (pull-based) :
  Pipeline → commit dans Git → ArgoCD pull → cluster
  Avantage : Git est la source de vérité unique
  Avantage : ArgoCD surveille en permanence les drifts
  Avantage : rollback = revert du commit Git
  Avantage : le pipeline n'a jamais d'accès direct au cluster
```

### Comprendre ArgoCD — le cycle de réconciliation

```
Toutes les 3 minutes (ou sur webhook Git) :
  ArgoCD lit l'état désiré dans Git
        ↓
  ArgoCD lit l'état réel dans K8s
        ↓
  Si identiques → "Synced" ✓
  Si différents → "OutOfSync" → applique les changements
                              → (selfHeal: true = automatique)
```

### Docker multi-stage — pourquoi c'est important

```
Sans multi-stage :
  Image finale contient : JDK + Maven + sources + code compilé
  Taille : 600-800 MB
  Surface d'attaque : tous les outils de build exposés

Avec multi-stage :
  Stage builder : JDK + Maven + sources + compile
  Stage runner  : JRE seul + JAR final
  Taille : 150-200 MB
  Surface d'attaque : minimale (pas de compiler, pas de sources)
```

### Commandes ArgoCD utiles

```bash
# Installer le CLI ArgoCD
brew install argocd  # macOS
# ou
curl -sSL -o argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64

# Se connecter
argocd login argocd.autoeecoleconnect.app

# Voir toutes les applications
argocd app list

# Forcer un sync
argocd app sync tenant-agence-lyon

# Voir l'historique de déploiements
argocd app history tenant-agence-lyon

# Rollback à une révision précédente
argocd app rollback tenant-agence-lyon 3

# Voir les diffs (état Git vs cluster)
argocd app diff tenant-agence-lyon
```

**Ressources :**
- [ArgoCD Docs — Getting Started](https://argo-cd.readthedocs.io/en/stable/getting_started/)
- [ApplicationSet — generators](https://argocd-applicationset.readthedocs.io/en/stable/Generators-Git/)
- [GitHub Actions — Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Docker multi-stage builds](https://docs.docker.com/build/building/multi-stage/)
- [GHCR — GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)

**Ce que ça t'apporte sur le CV :**
Configurer un pipeline GitOps complet — GitHub Actions → GHCR → ArgoCD ApplicationSet multi-tenant — c'est la pratique standard des équipes Platform Engineering modernes. Savoir que le cluster n'a jamais de credentials dans le pipeline (pull vs push), et pouvoir expliquer comment ArgoCD gère les drifts, c'est ce qui distingue un DevOps junior qui "a fait du CI/CD" d'un Platform Engineer qui comprend les principes.

---

