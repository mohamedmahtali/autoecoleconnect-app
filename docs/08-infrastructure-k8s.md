---
noteId: "38dac5527f8711f1878859078c773cc2"
tags: []

---

# 8. Infrastructure Kubernetes

## 8.1 Vue d'ensemble du cluster

```
Hetzner AX41 (bare metal)
├── OS : Ubuntu 24.04 LTS
├── Runtime : containerd
└── Kubernetes (kubeadm)
    │
    ├── kube-system/
    │   ├── cilium (CNI + Gateway API)
    │   ├── coredns
    │   └── kube-proxy (désactivé — Cilium le remplace)
    │
    ├── cert-manager/
    │   └── cert-manager (TLS automatique)
    │
    ├── sealed-secrets/
    │   └── sealed-secrets-controller
    │
    ├── cnpg-system/
    │   └── cloudnativepg-operator
    │
    ├── keda/
    │   └── keda-operator
    │
    ├── velero/
    │   └── velero
    │
    ├── monitoring/
    │   ├── prometheus
    │   ├── loki
    │   ├── grafana
    │   └── alertmanager
    │
    ├── argocd/
    │   └── argocd-server
    │
    ├── platform/                    ← Control Plane
    │   ├── frontend (Next.js)
    │   ├── backend (Spring Boot)
    │   ├── postgresql (CloudNativePG)
    │   └── rabbitmq
    │
    ├── agence-lyon/                 ← Tenant 1
    │   ├── frontend
    │   ├── backend
    │   └── postgresql
    │
    └── agence-paris/                ← Tenant 2
        ├── frontend
        ├── backend
        └── postgresql
```

---

## 8.2 Bootstrap du serveur — Ansible

Avant d'installer K8s, il faut préparer le serveur bare metal. Ansible automatise tout.

```yaml
# ansible/playbooks/bootstrap-server.yml

- name: Préparer le serveur Hetzner pour Kubernetes
  hosts: hetzner
  become: true
  roles:
    - common        # packages de base, firewall UFW, sysctl
    - containerd    # runtime de conteneurs
    - kubernetes    # kubelet, kubeadm, kubectl

# ansible/roles/common/tasks/main.yml
- name: Désactiver le swap (requis par K8s)
  command: swapoff -a

- name: Configurer sysctl pour K8s
  sysctl:
    name: "{{ item.key }}"
    value: "{{ item.value }}"
  loop:
    - { key: net.bridge.bridge-nf-call-iptables, value: 1 }
    - { key: net.ipv4.ip_forward,                value: 1 }
    - { key: net.bridge.bridge-nf-call-ip6tables, value: 1 }

- name: Charger les modules kernel
  modprobe:
    name: "{{ item }}"
  loop:
    - overlay
    - br_netfilter

# ansible/roles/kubernetes/tasks/main.yml
- name: Ajouter le repo Kubernetes
  apt_repository:
    repo: "deb https://pkgs.k8s.io/core:/stable:/v1.31/deb/ /"

- name: Installer kubelet, kubeadm, kubectl
  apt:
    name:
      - kubelet=1.31.*
      - kubeadm=1.31.*
      - kubectl=1.31.*
    state: present

- name: Verrouiller les versions K8s (éviter les upgrades accidentels)
  dpkg_selections:
    name: "{{ item }}"
    selection: hold
  loop: [kubelet, kubeadm, kubectl]
```

---

## 8.3 Initialisation du cluster — kubeadm

```bash
# Initialiser le control plane
# --pod-network-cidr : plage IP pour les pods (Cilium)
# --skip-phases : on n'installe pas kube-proxy (Cilium le remplace)
kubeadm init \
  --pod-network-cidr=10.244.0.0/16 \
  --skip-phases=addon/kube-proxy \
  --apiserver-advertise-address=<IP_HETZNER>

# Configurer kubectl pour l'utilisateur courant
mkdir -p $HOME/.kube
cp /etc/kubernetes/admin.conf $HOME/.kube/config

# Vérifier que le control plane est up
kubectl get nodes              # STATUS = NotReady (normal — pas encore de CNI)
kubectl get pods -n kube-system
```

---

## 8.4 Installation Cilium — CNI + Gateway API

```bash
# Installer Cilium CLI
CILIUM_CLI_VERSION=$(curl -s https://raw.githubusercontent.com/cilium/cilium-cli/main/stable.txt)
curl -L --remote-name https://github.com/cilium/cilium-cli/releases/download/${CILIUM_CLI_VERSION}/cilium-linux-amd64.tar.gz
tar xzvf cilium-linux-amd64.tar.gz -C /usr/local/bin

# Installer Cilium avec Gateway API activé
# --set kubeProxyReplacement=true : Cilium remplace kube-proxy (eBPF natif)
cilium install \
  --set kubeProxyReplacement=true \
  --set gatewayAPI.enabled=true \
  --set hubble.relay.enabled=true \
  --set hubble.ui.enabled=true

# Vérifier l'installation
cilium status
kubectl get nodes   # STATUS = Ready ✅

# Installer les CRDs Gateway API (requis par Cilium Gateway API)
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.1.0/standard-install.yaml
```

**Hubble UI** — visualisation du trafic réseau en temps réel :
```bash
cilium hubble ui   # ouvre http://localhost:12000
# Tu vois tous les flux entre pods — idéal pour débugger les NetworkPolicy
```

---

## 8.5 NetworkPolicy — isolation entre tenants

C'est la règle de sécurité la plus importante. Chaque namespace tenant est hermétique.

```yaml
# helm/portail-tenant/templates/networkpolicy.yaml
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: tenant-isolation
  namespace: {{ .Values.tenant.slug }}
spec:
  endpointSelector: {}      # s'applique à tous les pods du namespace

  ingress:
    # Autoriser uniquement le trafic depuis Cilium Gateway (trafic externe)
    - fromEntities:
        - cluster
      toPorts:
        - ports:
            - port: "3000"    # Next.js
              protocol: TCP
            - port: "8080"    # Spring Boot
              protocol: TCP

    # Autoriser Prometheus à scraper les métriques
    - fromNamespaces:
        - matchLabels:
            kubernetes.io/metadata.name: monitoring

  egress:
    # Autoriser uniquement les appels vers PostgreSQL local et RabbitMQ platform
    - toEndpoints:
        - matchLabels:
            app: postgres
    - toNamespaces:
        - matchLabels:
            kubernetes.io/metadata.name: platform
      toPorts:
        - ports:
            - port: "5672"    # RabbitMQ AMQP
              protocol: TCP

    # Autoriser DNS et internet (S3, Stripe API, Resend)
    - toEntities:
        - world
      toPorts:
        - ports:
            - port: "443"
              protocol: TCP
```

**Résultat :** le pod Spring Boot de `agence-lyon` ne peut jamais atteindre les pods de `agence-paris`. Même si un bug survient, l'accès inter-tenant est impossible au niveau kernel.

---

## 8.6 Cilium Gateway API — routing par tenant

```yaml
# Une Gateway partagée pour tout le cluster
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: autoeecoleconnect-gateway
  namespace: platform
spec:
  gatewayClassName: cilium
  listeners:
    - name: https
      port: 443
      protocol: HTTPS
      tls:
        certificateRefs:
          - name: wildcard-tls       # cert wildcard *.autoeecoleconnect.app
      hostname: "*.autoeecoleconnect.app"

---
# Une HTTPRoute par tenant (générée par le Helm chart)
# helm/portail-tenant/templates/httproute.yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: route-{{ .Values.tenant.slug }}
  namespace: {{ .Values.tenant.slug }}
spec:
  parentRefs:
    - name: autoeecoleconnect-gateway
      namespace: platform
  hostnames:
    - "{{ .Values.tenant.slug }}.autoeecoleconnect.app"
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /api
      backendRefs:
        - name: backend
          port: 8080
    - matches:
        - path:
            type: PathPrefix
            value: /
      backendRefs:
        - name: frontend
          port: 3000
```

---

## 8.7 cert-manager + Let's Encrypt (wildcard TLS)

```bash
# Installer cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.15.0/cert-manager.yaml

# Vérifier
kubectl get pods -n cert-manager
```

```yaml
# ClusterIssuer Let's Encrypt avec challenge DNS-01 via Cloudflare
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@autoeecoleconnect.app
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
      - dns01:
          cloudflare:
            email: admin@autoeecoleconnect.app
            apiTokenSecretRef:
              name: cloudflare-api-token
              key: api-token

---
# Certificate wildcard *.autoeecoleconnect.app
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: wildcard-autoeecoleconnect
  namespace: platform
spec:
  secretName: wildcard-tls
  dnsNames:
    - "autoeecoleconnect.app"
    - "*.autoeecoleconnect.app"
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
```

cert-manager renouvelle automatiquement le certificat 30 jours avant expiration. Zéro intervention manuelle.

---

## 8.8 Auto-scaling — KEDA + HPA

**Point important :** KEDA crée son propre HPA en interne. Il ne faut donc **jamais** déclarer un HPA manuel sur le même Deployment qu'un ScaledObject — les deux se battraient pour le contrôle des replicas. On met tous les triggers (CPU **et** RabbitMQ) dans le ScaledObject :

```yaml
# KEDA ScaledObject — un seul objet pour tous les triggers
# (KEDA génère l'HPA sous-jacent automatiquement)
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: backend-keda
  namespace: {{ .Values.tenant.slug }}
spec:
  scaleTargetRef:
    name: backend
  minReplicaCount: 1
  maxReplicaCount: 5
  triggers:
    # Trigger 1 — CPU (équivalent HPA classique)
    - type: cpu
      metricType: Utilization
      metadata:
        value: "70"       # scale up si CPU moyen > 70%
    # Trigger 2 — longueur de queue RabbitMQ
    - type: rabbitmq
      metadata:
        host: amqp://rabbitmq.platform.svc.cluster.local
        queueName: reservations-{{ .Values.tenant.slug }}
        queueLength: "10"   # scale up si > 10 messages en attente
```

Pour une auto-école avec une seule instance Spring Boot, ces scalers sont rarement déclenchés. Ils entrent en jeu lors de pics (rentrée scolaire, examens).

---

## 8.9 Backup — CloudNativePG + Velero

```yaml
# Backup nightly PostgreSQL vers Hetzner Object Storage
apiVersion: postgresql.cnpg.io/v1
kind: ScheduledBackup
metadata:
  name: postgres-backup
  namespace: {{ .Values.tenant.slug }}
spec:
  schedule: "0 2 * * *"          # chaque nuit à 2h00
  backupOwnerReference: self
  cluster:
    name: postgres-{{ .Values.tenant.slug }}
  target: prefer-standby          # backup depuis le replica si disponible
```

```bash
# Velero — snapshot complet du namespace chaque semaine
velero schedule create weekly-tenant-backup \
  --schedule="0 3 * * 0" \
  --include-namespaces agence-lyon,agence-paris \
  --storage-location hetzner-s3

# Restaurer un tenant complet
velero restore create \
  --from-backup weekly-tenant-backup-20260706 \
  --include-namespaces agence-lyon
```

---

## 8.10 Limites connues — choix assumés

| Limite | Risque | Plan de mitigation |
|---|---|---|
| **Un seul nœud** (AX41) | etcd et control plane sans HA — une panne matérielle = plateforme down | Backups etcd quotidiens + Velero. Migration 3 nœuds dès ~15 tenants payants (le CA le finance) |
| **PostgreSQL 1 instance/tenant** | Pas de réplica — restauration depuis backup S3 en cas de crash (RTO ~15 min) | `instances: 2` (réplica streaming CNPG) pour les plans Groupe/Réseau |
| **RabbitMQ single node** | Perte des messages en vol lors d'un crash | Queues durables + publisher confirms ; quorum queues si multi-nœuds |
| **ArgoCD poll 3 min** | Latence de provisioning | Webhook GitHub → ArgoCD pour un sync immédiat |

Ces limites sont documentées volontairement : une architecture qui explique ses trade-offs est plus crédible qu'une architecture qui prétend tout couvrir.

---

## 🎓 Montée en compétence — Administration K8s

### Les commandes kubectl indispensables

```bash
# Inspecter un namespace tenant
kubectl get all -n agence-lyon
kubectl describe pod backend-xxx -n agence-lyon
kubectl logs -f backend-xxx -n agence-lyon
kubectl exec -it backend-xxx -n agence-lyon -- /bin/sh

# Voir les événements K8s (diagnostiquer un pod qui ne démarre pas)
kubectl get events -n agence-lyon --sort-by='.lastTimestamp'

# Vérifier les NetworkPolicy actives
kubectl get ciliumnetworkpolicy -n agence-lyon

# Vérifier le certificat TLS
kubectl describe certificate wildcard-autoeecoleconnect -n platform
kubectl get certificaterequest -n platform

# Vérifier le statut CloudNativePG
kubectl get cluster -n agence-lyon
kubectl describe cluster postgres-agence-lyon -n agence-lyon

# Vérifier les backups
kubectl get scheduledbackup -n agence-lyon
kubectl get backup -n agence-lyon
```

### Comprendre kubeadm — les composants installés

```
kubeadm init installe :

etcd              → BDD clé-valeur qui stocke tout l'état du cluster
                    (pods, services, configmaps, secrets...)
                    Critique : si etcd tombe, le cluster ne fonctionne plus

kube-apiserver    → point d'entrée de toutes les commandes kubectl
                    C'est lui qui lit/écrit dans etcd

kube-scheduler    → décide sur quel nœud placer chaque pod
                    (selon les ressources disponibles, les affinités, etc.)

kube-controller-  → boucle de réconciliation
manager             "l'état réel correspond-il à l'état désiré ?"
                    Si non → prend des actions correctives
                    (recréer un pod mort, scaler un deployment, etc.)
```

**Ressources :**
- [Kubernetes The Hard Way](https://github.com/kelseyhightower/kubernetes-the-hard-way) — installer K8s de zéro (formation intensive)
- [Cilium Labs](https://isovalent.com/resource-library/labs/) — labs interactifs gratuits
- [CloudNativePG Docs](https://cloudnative-pg.io/docs/) — gestion PostgreSQL en K8s
- [KEDA Docs](https://keda.sh/docs/) — scaling event-driven

**Ce que ça t'apporte sur le CV :**
Avoir installé kubeadm de zéro avec Cilium, configuré des NetworkPolicy eBPF, opéré CloudNativePG avec backups S3 et mis en place cert-manager pour un wildcard TLS automatique — c'est le profil d'un Platform Engineer junior/mid-level. Ces compétences s'apprennent rarement en formation : elles viennent de la pratique sur un vrai cluster.

---

