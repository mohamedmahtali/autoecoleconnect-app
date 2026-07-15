---
noteId: "38dac5537f8711f1878859078c773cc2"
tags: []

---

# 9. Cycle de vie d'un tenant

Chaque tenant traverse des états bien définis depuis la création jusqu'à la suppression. Ces transitions sont pilotées par le Control Plane et exécutées via GitOps (ArgoCD).

## 9.1 États et transitions

```
                ┌─────────────────────────────────────────────────────────┐
                │              Cycle de vie d'un tenant                   │
                └─────────────────────────────────────────────────────────┘

  [Inscription]
       │
       ▼
 ┌────────────┐    GitHub API commit     ┌──────────────┐
 │provisioning│ ──────────────────────► │              │
 │            │    ArgoCD sync (~2min)   │   TRIAL      │
 └────────────┘ ◄──────────────────────  │  (30 jours)  │
       │              namespace créé      │              │
       │                                 └──────┬───────┘
       │                                        │
       │                              J+25 → email rappel
       │                                        │
       │                              J+30 → suspension auto
       │                                        │
       │                                        ▼
       │                              ┌──────────────────┐
       │   Paiement Stripe reçu       │                  │
       │ ◄────────────────────────── │   SUSPENDED      │
       │                              │  (accès coupé)   │
       ▼                              └──────┬───────────┘
 ┌────────────┐                             │
 │            │   Résiliation / non-paiement│  J+60 → suppression
 │   ACTIVE   │ ◄───────────────────────── │
 │  (facturation│                            ▼
 │  mensuelle) │                   ┌──────────────────┐
 └─────┬──────┘                    │                  │
       │                           │    DELETED       │
       │ Résiliation volontaire     │  namespace purgé │
       └──────────────────────────►│  backup conservé │
                                   │  90 jours RGPD   │
                                   └──────────────────┘
```

| État | Description | Accès tenant | Facturation |
|---|---|---|---|
| `provisioning` | Namespace K8s en cours de création | Non | Non |
| `trial` | Période d'essai 30 jours | Complet | Non |
| `active` | Abonnement Stripe actif | Complet | Oui |
| `suspended` | Essai expiré ou paiement échoué | Lecture seule | Non |
| `deleted` | Namespace purgé, données anonymisées | Non | Non |

---

## 9.2 J+0 — Provisioning automatique

Quand une auto-école s'inscrit et choisit un plan, le flow suivant se déclenche :

```
[Formulaire inscription autoeecoleconnect.app]
    │  POST /api/provision
    ▼
[Control Plane — ProvisioningService]
    │
    ├── 1. Crée organisation + tenant en BDD (statut: provisioning)
    │
    ├── 2. Génère values.yaml pour le tenant
    │       slug: agence-lyon
    │       namespace: agence-lyon
    │       url: lyon.autoeecoleconnect.app
    │       plan: solo
    │       trial_ends_at: 2026-08-11
    │
    ├── 3. Appel GitHub API — commit values.yaml dans autoeecoleconnect-infra/
    │       POST /repos/org/autoeecoleconnect-infra/contents/tenants/agence-lyon/values.yaml
    │
    ├── 4. ArgoCD détecte le commit (sync toutes les 3 minutes)
    │       └── kubectl apply -n agence-lyon (Namespace + Deployment + Service + HTTPRoute...)
    │
    ├── 5. CloudNativePG crée le PostgreSQL (~90s)
    │
    ├── 6. Webhook ArgoCD → Control Plane : "sync succeeded"
    │       └── UPDATE tenants SET statut = 'trial' WHERE slug = 'agence-lyon'
    │
    └── 7. Email Resend → directeur : "Votre espace est prêt"
                          URL : https://lyon.autoeecoleconnect.app
                          Identifiants directeur initiaux
```

Durée totale : **2 à 4 minutes** de la soumission du formulaire à la réception de l'email.

---

## 9.3 J+25 — Rappel d'essai

Un CronJob K8s s'exécute chaque nuit et envoie les rappels :

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: trial-reminders
  namespace: platform
spec:
  schedule: "0 8 * * *"    # chaque matin à 8h00
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: reminder
              image: ghcr.io/autoeecoleconnect/control-plane:latest
              command: ["java", "-jar", "app.jar", "--job=trial-reminder"]
          restartPolicy: OnFailure
```

```
Logic du job (trial_ends_at et reminder_sent sont sur organisations — cf. section 6.2) :
  1. SELECT t.* FROM tenants t
       JOIN organisations o ON o.id = t.org_id
       WHERE t.statut = 'trial'
       AND o.trial_ends_at BETWEEN NOW() AND NOW() + INTERVAL '6 days'
       AND o.reminder_sent = false
  2. Pour chaque organisation → envoie email Resend "Votre essai se termine dans X jours"
  3. UPDATE organisations SET reminder_sent = true
```

---

## 9.4 J+30 — Suspension automatique

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: trial-suspender
  namespace: platform
spec:
  schedule: "0 9 * * *"    # chaque matin à 9h00
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: suspender
              image: ghcr.io/autoeecoleconnect/control-plane:latest
              command: ["java", "-jar", "app.jar", "--job=trial-suspend"]
          restartPolicy: OnFailure
```

Quand un tenant expire, le Control Plane le **suspend** (pas supprime) :

```java
@Component
public class TrialSuspendJob {

    public void run() {
        List<Tenant> expired = tenantRepo.findExpiredTrials();

        for (Tenant tenant : expired) {
            // 1. Couper l'accès — patch Deployment replicas=0 via Kubernetes API
            k8sClient.scale(tenant.getNamespace(), "frontend", 0);
            k8sClient.scale(tenant.getNamespace(), "backend", 0);

            // 2. Mettre à jour le statut en BDD
            tenant.setStatut("suspended");
            tenant.setSuspendedAt(Instant.now());
            tenantRepo.save(tenant);

            // 3. Envoyer l'email "Votre essai est terminé"
            resendService.sendTrialExpired(tenant);

            // 4. Log d'audit
            provisioningLogRepo.save(new ProvisioningLog(tenant, "suspend", "success"));
        }
    }
}
```

**Ce que voit le directeur quand il se connecte :**
```
┌──────────────────────────────────────────────────────┐
│  Votre période d'essai est terminée                  │
│                                                      │
│  Choisissez un plan pour continuer à utiliser        │
│  AutoEcoleConnect et conserver toutes vos données.         │
│                                                      │
│  [Solo — 29€/mois]  [Pro — 59€/mois]               │
│                                                      │
│  Vos données sont conservées 60 jours.              │
└──────────────────────────────────────────────────────┘
```

---

## 9.5 Réactivation — paiement Stripe reçu

```
[Directeur clique "Choisir le plan Pro"]
    │
    ▼
[Control Plane → Stripe Subscriptions]
    │  checkout.session.completed webhook
    ▼
[WebhookController → ActivationService]
    │
    ├── 1. Vérifier idempotence (stripe_event_id déjà traité ?)
    ├── 2. UPDATE organisations SET plan = 'pro', stripe_subscription_id = '...'
    ├── 3. UPDATE tenants SET statut = 'active'
    ├── 4. Patch K8s : replicas=1 pour frontend + backend
    └── 5. Email Resend : "Bienvenue sur AutoEcoleConnect Pro !"
```

Le tenant est accessible à nouveau **en moins d'une minute** après le paiement.

---

## 9.6 Suspension pour non-paiement (tenant actif)

Stripe envoie `invoice.payment_failed` si le renouvellement mensuel échoue :

```
Stripe → invoice.payment_failed
    │
    ▼
Control Plane → grace period 10 jours (cf. section 5.5)
    │  J+3, J+7 → tentatives de prélèvement automatiques Stripe + emails
    │
    ▼  Si toujours impayé après 10 jours
suspend → replicas=0 (même logique que fin d'essai)
    │
    ▼  Si toujours impayé après 30 jours supplémentaires
delete → namespace purgé
```

---

## 9.7 J+60 — Suppression définitive

```java
@Component
public class TenantDeleteJob {

    public void run() {
        // Tenants suspendus depuis > 60 jours sans paiement
        List<Tenant> toDelete = tenantRepo.findSuspendedOlderThan(Duration.ofDays(60));

        for (Tenant tenant : toDelete) {
            // 1. Supprimer le namespace K8s (supprime tout : pods, PVC, secrets)
            k8sClient.deleteNamespace(tenant.getNamespace());

            // 2. Supprimer le fichier values.yaml du repo GitOps
            githubService.deleteFile(
                "autoeecoleconnect-infra",
                "tenants/" + tenant.getSlug() + "/values.yaml",
                "chore: remove tenant " + tenant.getSlug()
            );

            // 3. Anonymiser les données BDD Control Plane (RGPD)
            tenant.setNom("Tenant supprimé");
            tenant.setDeletedAt(Instant.now());
            tenantRepo.save(tenant);

            // 4. Les backups Velero sont conservés 90 jours (RGPD)
            // → pas de suppression immédiate des backups
        }
    }
}
```

**Backups après suppression :**
```
Velero backup → Hetzner Object Storage
  Politique de rétention :
    - Backups tenant actif : 30 jours glissants
    - Backups tenant supprimé : 90 jours (obligation RGPD)
    - Après 90 jours : suppression automatique (Hetzner lifecycle rule)
```

---

## 9.8 Tableau de bord gérant — vue multi-tenants

Le gérant avec plusieurs auto-écoles voit un dashboard consolidé dans le Control Plane :

```
┌──────────────────────────────────────────────────────────────────┐
│  Tableau de bord — Groupe Martin Auto-École                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  CA total juillet 2026 : 18 420 €                               │
│  ████████████████████████████████████████  (+12% vs juin)       │
│                                                                  │
│  ┌──────────────────┬───────────┬──────────┬──────────────────┐ │
│  │ Auto-école       │ Statut    │ CA/mois  │ Élèves actifs    │ │
│  ├──────────────────┼───────────┼──────────┼──────────────────┤ │
│  │ Lyon Centre      │ ● Actif   │ 8 200 €  │ 47               │ │
│  │ Lyon Villeurbanne│ ● Actif   │ 6 420 €  │ 38               │ │
│  │ Grenoble         │ ● Actif   │ 3 800 €  │ 22               │ │
│  └──────────────────┴───────────┴──────────┴──────────────────┘ │
│                                                                  │
│  [Accéder à Lyon Centre]  [Accéder à Villeurbanne]              │
└──────────────────────────────────────────────────────────────────┘
```

Ces métriques proviennent des événements RabbitMQ publiés par chaque tenant (jamais d'accès direct entre BDD tenants et Control Plane).

---

## 🎓 Montée en compétence — Automatisation K8s

### CronJobs Kubernetes vs cron Linux

Un CronJob K8s est préférable à un cron système pour plusieurs raisons :

```
cron Linux (classique) :
  - Tourne sur une seule machine
  - Si la machine tombe → le job ne s'exécute pas
  - Logs perdus ou à chercher dans /var/log
  - Pas de retry automatique

CronJob Kubernetes :
  - K8s schedule le pod sur n'importe quel nœud disponible
  - Retry configurable (restartPolicy: OnFailure)
  - Logs accessibles via kubectl logs
  - Historique des exécutions (successfulJobsHistoryLimit)
  - Alertes Prometheus si le job échoue
```

### Kubernetes API depuis une application Java

```java
// Dépendance Maven
<dependency>
    <groupId>io.kubernetes</groupId>
    <artifactId>client-java</artifactId>
    <version>20.0.0</version>
</dependency>

// Authentification dans le cluster (ServiceAccount monté automatiquement)
ApiClient client = ClientBuilder.cluster().build();
Configuration.setDefaultApiClient(client);

AppsV1Api appsApi = new AppsV1Api();

// Scale un Deployment à 0 (suspension)
V1Scale scale = new V1Scale()
    .spec(new V1ScaleSpec().replicas(0));

appsApi.replaceNamespacedDeploymentScale(
    "backend",          // nom du deployment
    "agence-lyon",      // namespace
    scale,
    null, null, null, null
);
```

Le pod qui exécute ce code doit avoir un **ServiceAccount** avec les droits RBAC appropriés :

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: tenant-manager
rules:
  - apiGroups: ["apps"]
    resources: ["deployments", "deployments/scale"]
    verbs: ["get", "patch", "update"]
  - apiGroups: [""]
    resources: ["namespaces"]
    verbs: ["get", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: control-plane-tenant-manager
subjects:
  - kind: ServiceAccount
    name: control-plane
    namespace: platform
roleRef:
  kind: ClusterRole
  name: tenant-manager
  apiGroup: rbac.authorization.k8s.io
```

### Idempotence — principe fondamental

Tous les jobs de cycle de vie doivent être idempotents : les relancer deux fois ne doit pas avoir d'effet de bord.

```java
// ❌ Non idempotent — envoie deux emails si le job tourne deux fois
if (tenant.isExpired()) {
    sendEmail(tenant);
    tenant.setReminderSent(true);
}

// ✅ Idempotent — vérifie avant d'agir
if (tenant.isExpired() && !tenant.isReminderSent()) {
    sendEmail(tenant);
    tenant.setReminderSent(true);  // garde en BDD
}
```

**Pourquoi c'est critique** : un CronJob K8s peut être relancé en cas de crash du nœud. Sans idempotence, un directeur reçoit 3 emails "votre essai expire bientôt" le même matin.

**Ressources :**
- [Kubernetes CronJob docs](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/)
- [client-java — SDK Kubernetes pour Java](https://github.com/kubernetes-client/java)
- [RBAC Authorization](https://kubernetes.io/docs/reference/access-authn-authz/rbac/)
- [Stripe — gérer les paiements échoués](https://stripe.com/docs/billing/subscriptions/overview#subscription-lifecycle)

**Ce que ça t'apporte sur le CV :**
Concevoir et implémenter le cycle de vie complet d'un tenant (provisioning → trial → active → suspend → delete) avec GitOps, K8s API et RBAC fine-grained montre une maîtrise de bout en bout de la plateforme. C'est exactement ce que font les Platform Engineers dans les scale-ups SaaS.

---

