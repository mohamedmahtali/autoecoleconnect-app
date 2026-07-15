---
noteId: "38dac5517f8711f1878859078c773cc2"
tags: []

---

# 5. Modèle Business

## 5.1 Les deux niveaux de facturation

C'est le point le plus important à ne pas confondre dans l'architecture paiement :

```
Niveau 1 — B2C : élève → auto-école
  L'élève paye son forfait de conduite à l'auto-école
  PaymentIntents Stripe / PayPlug / Alma / espèces
  L'argent va sur le compte de l'auto-école
  Géré par le portail tenant (pod Spring Boot de l'auto-école)

Niveau 2 — B2B : auto-école → AutoEcoleConnect (plateforme)
  L'auto-école paye son abonnement mensuel à la plateforme
  Stripe Subscriptions
  L'argent va sur ton compte AutoEcoleConnect
  Géré par le Control Plane (Provisioning Service)
```

**Ces deux flux ne se mélangent jamais.** Comptes Stripe distincts, code séparé, BDD séparée.

---

## 5.2 Plans tarifaires détaillés

```
┌────────────────┬──────────┬──────────┬──────────┬───────────┐
│                │  Solo    │   Pro    │  Groupe  │  Réseau   │
├────────────────┼──────────┼──────────┼──────────┼───────────┤
│ Prix/mois      │  29 €    │  59 €    │  99 €    │ Sur devis │
│ Auto-écoles    │    1     │    1     │  jusqu'à │ Illimitées│
│                │          │          │    5     │           │
│ Moniteurs      │ max 2    │ Illimité │ Illimité │ Illimité  │
│ Clients actifs │ max 50   │ Illimité │ Illimité │ Illimité  │
├────────────────┼──────────┼──────────┼──────────┼───────────┤
│ Paiement manuel│    ✅    │    ✅    │    ✅    │    ✅     │
│ Stripe         │    ❌    │    ✅    │    ✅    │    ✅     │
│ PayPlug        │    ❌    │    ✅    │    ✅    │    ✅     │
│ Alma (BNPL)    │    ❌    │    ✅    │    ✅    │    ✅     │
│ CPF            │    ❌    │    ✅    │    ✅    │    ✅     │
├────────────────┼──────────┼──────────┼──────────┼───────────┤
│ Stats avancées │    ❌    │    ✅    │    ✅    │    ✅     │
│ Dashboard CA   │    ❌    │    ❌    │    ✅    │    ✅     │
│ consolidé      │          │  (1 agence)│multi  │ multi     │
│ Support        │  Email   │Prioritaire│  Dédié  │  SLA      │
├────────────────┼──────────┼──────────┼──────────┼───────────┤
│ K8s resources  │  Small   │  Medium  │  Medium  │  Large    │
│ CPU limit      │  500m    │  1000m   │  1000m   │ Négocié   │
│ RAM limit      │  512Mi   │   1Gi    │   1Gi    │ Négocié   │
│ Storage PG     │   5Gi    │  10Gi    │  10Gi    │ Négocié   │
└────────────────┴──────────┴──────────┴──────────┴───────────┘
```

Les limites de plan sont stockées dans la BDD du Control Plane et vérifiées par le Spring Boot du tenant à chaque opération critique (création de moniteur, ajout de client).

---

## 5.3 Cycle de vie d'un abonnement

```
Inscription (J+0)
  └─ namespace créé, flag trial=true, expiry=J+30
         │
         ▼
  Trial actif (J+0 → J+30)
  └─ accès complet à tous les modules (démonstration)
         │
    ┌────┴────────────────────────┐
    │                             │
  Paiement effectué          Pas de paiement
    │                             │
    ▼                             ▼
  Abonné actif              J+25 : email rappel
  (plan choisi)             J+30 : namespace suspendu
         │                     pods stoppés
         │                     données conservées
         │                        │
         │                    J+60 : namespace supprimé
         │                     git rm tenants/agence-xxx/
         │                     ArgoCD sync → namespace deleted
         │                     email confirmation RGPD
         ▼
  Renouvellement mensuel (Stripe Subscription)
         │
    Paiement échoue → email → 3 tentatives → suspension
```

---

## 5.4 Modèle Organisation → Tenants

Un même gérant peut posséder plusieurs auto-écoles sous une organisation unique.

```
BDD Control Plane :

organisations
  id          uuid PK
  nom         "Groupe Martin"
  plan        "groupe"
  stripe_subscription_id
  statut      active | trial | suspended | deleted
  trial_ends_at                  ← le trial est lié à l'abonnement, donc à l'organisation
  created_at

tenants
  id          uuid PK
  org_id      uuid FK → organisations
  slug        "agence-lyon"
  namespace   "agence-lyon"      ← nom du namespace K8s
  nom         "Auto-École Lyon Centre"
  url         "lyon.autoeecoleconnect.app"
  statut      provisioning | trial | active | suspended | deleted
  created_at

org_metrics                       ← agrégé depuis événements RabbitMQ
  org_id      uuid FK
  mois        "2026-07"
  ca_total    decimal
  nb_eleves   integer
  nb_reservations integer
  updated_at
```

**Flux d'agrégation des métriques :**

```
Tenant Lyon (Spring Boot)
  → publie ReservationPaid { montant: 1500€, org_id: "groupe-martin" }
  → publie sur RabbitMQ exchange: "metrics"

Control Plane (consommateur RabbitMQ)
  → reçoit l'événement
  → UPDATE org_metrics SET ca_total = ca_total + 1500
    WHERE org_id = 'groupe-martin' AND mois = '2026-07'

Dashboard gérant
  → GET /api/organisations/groupe-martin/metrics
  → affiche CA total = 2 300 € ce mois
     Lyon : 1 500 € | Paris : 800 €
```

Les tenants n'exposent jamais leurs données directement au Control Plane — ils publient uniquement des **événements agrégés** (montants, compteurs). Isolation préservée.

---

## 5.5 Billing B2B — Stripe Subscriptions

L'auto-école souscrit un plan via Stripe. La logique est dans le Control Plane, pas dans le portail tenant.

```
Flux de souscription :

1. Gérant choisit le plan Pro (59€/mois) sur autoeecoleconnect.app
2. Redirect vers Stripe Checkout (Stripe hébergé)
3. Stripe débite la CB → webhook checkout.session.completed
4. Control Plane (Spring Boot) reçoit le webhook :
   - Met à jour organisations.stripe_subscription_id
   - Met à jour tenant.statut = "active"
   - Met à jour tenant.plan = "pro"
   - Publie PlanActivated → RabbitMQ
5. Portail tenant reçoit PlanActivated :
   - Active les modules Pro (Stripe, PayPlug, Alma, stats)
   - Met à jour les ResourceQuota K8s (CPU/RAM Medium)

Flux de renouvellement mensuel (automatique Stripe) :
  Stripe débite → webhook invoice.payment_succeeded → statut reste active

Flux d'échec paiement :
  Stripe échoue → webhook invoice.payment_failed
  → email gérant (tentative 1)
  → J+3 : 2ème tentative automatique Stripe
  → J+7 : 3ème tentative + email urgent
  → J+10 : suspension namespace
```

---

## 5.6 Coûts d'infrastructure & break-even

| Poste | Coût mensuel |
|---|---|
| Hetzner AX41 (bare metal) | ~55 € |
| Hetzner Object Storage (backups, ~250 GB) | ~12 € |
| Cloudflare (Free tier) | 0 € |
| Resend (jusqu'à 50k emails/mois) | ~18 € |
| Domaine autoeecoleconnect.app | ~2 € |
| GitHub (Actions + GHCR, free tier) | 0 € |
| **Total** | **~87 €/mois** |

```
Break-even  : 3 clients Solo (87 €) ou 2 clients Pro (118 €)
Capacité    : 20-25 tenants sur un AX41 → CA potentiel 580-1 500 €/mois
Marge à pleine capacité : ~85-94 %
```

Le serveur suivant (AX41 supplémentaire ou passage à 3 nœuds) n'est nécessaire qu'à partir de ~20 tenants — il est alors largement autofinancé.

---

## 🎓 Montée en compétence — SaaS Billing & Stripe

### Comprendre Stripe Subscriptions vs PaymentIntents

C'est la confusion la plus fréquente quand on construit un SaaS avec Stripe :

| | PaymentIntent | Subscription |
|---|---|---|
| Usage | Paiement unique | Paiement récurrent |
| Exemple AutoEcoleConnect | Élève paye un forfait 1500€ | Auto-école paye 59€/mois |
| Webhook principal | `payment_intent.succeeded` | `invoice.payment_succeeded` |
| Annulation | Remboursement manuel | `subscription.deleted` |
| Retry automatique | Non | Oui (Smart Retries Stripe) |

### Webhooks Stripe — les essentiels à implémenter

```java
// Spring Boot - gestionnaire de webhooks Stripe
@PostMapping("/webhooks/stripe/platform")
public ResponseEntity<Void> handlePlatformWebhook(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String signature
) {
    Event event = Webhook.constructEvent(payload, signature, webhookSecret);

    switch (event.getType()) {
        case "checkout.session.completed"     -> provisioningService.activate(event);
        case "invoice.payment_succeeded"      -> subscriptionService.renew(event);
        case "invoice.payment_failed"         -> subscriptionService.handleFailure(event);
        case "customer.subscription.deleted"  -> provisioningService.suspend(event);
    }
}
```

**Point critique — idempotence :**
Stripe peut envoyer le même webhook plusieurs fois. Toujours vérifier si l'événement a déjà été traité avant d'agir :

```java
// Vérifier que l'event n'a pas déjà été traité
if (webhookEventRepository.existsByStripeEventId(event.getId())) {
    return ResponseEntity.ok().build(); // déjà traité, ignorer
}
webhookEventRepository.save(new WebhookEvent(event.getId()));
// traiter l'événement...
```

**Ressources :**
- [Stripe Docs — Subscriptions](https://stripe.com/docs/billing/subscriptions/overview)
- [Stripe Docs — Webhooks best practices](https://stripe.com/docs/webhooks/best-practices)
- [Stripe Testing](https://stripe.com/docs/testing) — cartes de test pour simuler succès/échec/retry

**Ce que ça t'apporte sur le CV :**
Implémenter un billing SaaS avec Stripe Subscriptions (webhooks, idempotence, retry logic, dunning) est une compétence très recherchée. La plupart des devs n'ont fait que des PaymentIntents basiques.

---

