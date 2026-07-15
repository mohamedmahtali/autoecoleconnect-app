---
noteId: "38da9e437f8711f1878859078c773cc2"
tags: []

---

# 7. Module Paiement

## 7.1 Vue d'ensemble — architecture multi-provider

Chaque auto-école configure ses propres moyens de paiement depuis son espace. La plateforme route automatiquement vers le bon provider.

```
Client (élève) choisit son moyen de paiement
        │
        ▼
Spring Boot — PaymentService (abstraction)
        │
        ├── config.stripe_enabled    → StripeProvider
        ├── config.payplug_enabled   → PayPlugProvider
        ├── config.alma_enabled      → AlmaProvider
        └── manuel                  → ManualPaymentHandler
                                       (espèces, chèque, virement, CPF)
```

L'auto-école active/désactive chaque provider depuis son dashboard. Les clés API sont stockées dans K8s Sealed Secrets — jamais en clair dans la BDD.

---

## 7.2 Providers en ligne

### Stripe

Le plus complet. Supporte les paiements CB, Apple Pay, Google Pay, virements SEPA.

```
Flux de paiement Stripe :

1. Client clique "Payer 1 500€"
2. Spring Boot crée un PaymentIntent (Stripe API)
   → retourne client_secret
3. Next.js affiche le formulaire Stripe Elements
   (formulaire CB hébergé par Stripe — PCI DSS géré)
4. Client saisit sa CB → paiement traité par Stripe
5. Stripe envoie webhook payment_intent.succeeded
6. Spring Boot (route /webhooks/stripe) :
   - Vérifie la signature Stripe (prévient les faux webhooks)
   - Vérifie idempotence (stripe_event_id déjà traité ?)
   - Met à jour reservations.paiement_statut = 'paid'
   - Publie ReservationPaid → RabbitMQ
   - Resend envoie email confirmation au client
```

Configuration par tenant (Sealed Secret K8s) :
```yaml
# sealed-secret-payments.yaml (chiffré dans autoeecoleconnect-infra/)
apiVersion: bitnami.com/v1alpha1
kind: SealedSecret
spec:
  encryptedData:
    STRIPE_SECRET_KEY: AgBy3i4OJSWK+PiTySYZZA9rO43cGDEq...
    STRIPE_WEBHOOK_SECRET: AgCWFJJI3kKnSfDLDGGKM3o4Cv...
    PAYPLUG_SECRET_KEY: AgAKs7Kz0m1o9DrLV3uB7z...
    ALMA_API_KEY: AgBk2MNs7Rm...
```

---

### PayPlug

Alternative française, bien adaptée aux PME. API REST similaire à Stripe.

```
Flux PayPlug :

1. Spring Boot crée un paiement PayPlug
   POST https://api.payplug.com/v1/payments
   → retourne une payment_url hébergée par PayPlug
2. Next.js redirige vers la page de paiement PayPlug
3. Client paye sur la page PayPlug (CB)
4. PayPlug envoie une IPN (Instant Payment Notification)
   POST vers /webhooks/payplug
5. Spring Boot traite l'IPN :
   - Vérifie la signature HMAC
   - Met à jour la réservation
   - Publie l'événement RabbitMQ
```

---

### Alma — Paiement en plusieurs fois (BNPL)

Très pertinent pour les auto-écoles : un forfait à 1 500€ devient 3 × 500€.

```
Plans Alma disponibles :
  P2X  → 2 fois sans frais
  P3X  → 3 fois sans frais  ← le plus utilisé en auto-école
  P4X  → 4 fois sans frais

Flux Alma :

1. Client choisit "Payer en 3 fois"
2. Spring Boot crée une session Alma
   POST https://api.getalma.eu/v1/payments
   body: { purchase_amount: 150000, payment_plan: [{due_date, purchase_amount}×3] }
   → retourne url de paiement Alma
3. Client valide sur la page Alma (scoring crédit instantané)
4. Alma valide et envoie webhook payment.on_payment_succeeded
5. Spring Boot traite :
   - Réservation activée dès le premier versement
   - Les versements suivants sont automatiques (Alma gère)
```

---

## 7.3 Méthodes manuelles

Les paiements hors ligne sont saisis manuellement par le directeur. Pas d'API externe — juste un formulaire dans l'interface admin.

```
┌─────────────────────────────────────────────────────────────┐
│  Saisie paiement manuel — interface directeur               │
├─────────────────────────────────────────────────────────────┤
│  Méthode :  ○ Espèces  ○ Chèque  ○ Virement  ○ CPF        │
│             ○ Permis 1€/jour  ○ Autre                       │
│                                                             │
│  Référence : [_____________________]                        │
│  (n° chèque, référence virement, n° dossier CPF...)        │
│                                                             │
│  Date paiement : [__/__/____]                               │
│  Montant reçu  : [________] €                               │
│  Caution réglée: ○ Oui  ○ Non                              │
│                                                             │
│  [Valider le paiement]                                      │
└─────────────────────────────────────────────────────────────┘
```

| Méthode | Référence stockée | Validation |
|---|---|---|
| Espèces | Optionnelle | Immédiate par le directeur |
| Chèque | N° de chèque | Après encaissement (délai 2-3j) |
| Virement SEPA | Référence virement | Après réception (délai 1-2j) |
| CPF | N° dossier Mon Compte Formation | Après confirmation CDC |
| Permis 1€/jour | N° dossier | Après accord organisme |

---

## 7.4 Abstraction provider — Spring Boot

Un seul `PaymentService` qui délègue au bon provider selon la configuration du tenant.

```java
// Interface commune à tous les providers
public interface PaymentProvider {
    PaymentSession createSession(PaymentRequest request);
    PaymentStatus getStatus(String providerPaymentId);
    RefundResult refund(String providerPaymentId, BigDecimal amount);
}

// Implémentations
@Service @ConditionalOnProperty("payment.stripe.enabled")
public class StripePaymentProvider implements PaymentProvider { ... }

@Service @ConditionalOnProperty("payment.payplug.enabled")
public class PayPlugPaymentProvider implements PaymentProvider { ... }

@Service @ConditionalOnProperty("payment.alma.enabled")
public class AlmaPaymentProvider implements PaymentProvider { ... }

// Service principal — route vers le bon provider
@Service
public class PaymentService {

    private final Map<String, PaymentProvider> providers;
    private final TenantConfig config;

    public PaymentSession initiate(PaymentRequest request) {
        String provider = request.getProvider(); // stripe | payplug | alma

        if (!config.isProviderEnabled(provider)) {
            throw new DomainException("Provider non activé sur ce tenant", 400);
        }

        return providers.get(provider).createSession(request);
    }
}
```

---

## 7.5 Webhooks — route unifiée par provider

```java
// Un endpoint par provider — signature vérifiée différemment selon le provider
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    @PostMapping("/stripe")
    public ResponseEntity<Void> stripe(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sig
    ) {
        webhookService.handleStripe(payload, sig);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payplug")
    public ResponseEntity<Void> payplug(
        @RequestBody String payload,
        @RequestHeader("X-Payplug-Signature") String sig
    ) {
        webhookService.handlePayPlug(payload, sig);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/alma")
    public ResponseEntity<Void> alma(
        @RequestBody String payload,
        @RequestHeader("X-Alma-Signature") String sig
    ) {
        webhookService.handleAlma(payload, sig);
        return ResponseEntity.ok().build();
    }
}
```

**Règles communes à tous les webhooks :**
1. Toujours vérifier la signature avant de traiter
2. Toujours vérifier l'idempotence (`provider_event_id` déjà en BDD ?)
3. Toujours retourner HTTP 200 rapidement (traitement en async via RabbitMQ)
4. Ne jamais exposer les erreurs internes dans la réponse

---

## 7.6 Affichage IBAN pour les virements

Quand un client choisit le virement SEPA, l'interface affiche l'IBAN de l'auto-école.

```
L'IBAN est configuré par le directeur dans les paramètres de son espace.
Il est stocké en BDD tenant (table config_paiement).
Il n'est jamais partagé avec d'autres tenants.

Affichage côté client :
  "Veuillez effectuer un virement à :
   Bénéficiaire : Auto-École Lyon Centre
   IBAN : FR76 3000 6000 0112 3456 7890 189
   Référence : RESERVATION-2026-001
   Votre réservation sera activée dès réception."
```

---

## 🎓 Montée en compétence — Intégration de paiement

### Les pièges classiques à éviter

**1. Ne jamais faire confiance au client pour le montant**
```java
// ❌ Dangereux — le montant vient du frontend
PaymentIntent.create(Map.of("amount", request.getAmount()));

// ✅ Correct — le montant vient de la BDD
Reservation reservation = reservationRepo.findById(id).orElseThrow();
PaymentIntent.create(Map.of("amount", reservation.getMontantCents()));
```

**2. Toujours vérifier la signature des webhooks**
```java
// ❌ Dangereux — n'importe qui peut POST sur /webhooks/stripe
Event event = objectMapper.readValue(payload, Event.class);

// ✅ Correct — Stripe signe chaque webhook avec ton secret
Event event = Webhook.constructEvent(payload, signature, webhookSecret);
```

**3. Traiter les webhooks de façon idempotente**
Stripe, PayPlug et Alma peuvent envoyer le même événement plusieurs fois (retry réseau, timeout). Toujours vérifier avant de traiter :
```java
if (paiementRepo.existsByProviderEventId(eventId)) {
    return; // déjà traité — ignorer silencieusement
}
```

### Tester les paiements en local

```bash
# Stripe CLI — écouter les webhooks en local
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# Déclencher un événement de test
stripe trigger payment_intent.succeeded

# Cartes de test Stripe
4242 4242 4242 4242  → paiement réussi
4000 0000 0000 9995  → paiement refusé (fonds insuffisants)
4000 0025 0000 3155  → 3D Secure requis
```

**Ressources :**
- [Stripe Testing — cartes de test](https://stripe.com/docs/testing)
- [PayPlug API Docs](https://docs.payplug.com/api/)
- [Alma API Docs](https://developers.getalma.eu/)
- [PCI DSS — pourquoi utiliser Stripe Elements](https://stripe.com/docs/security/guide)

**Ce que ça t'apporte sur le CV :**
Implémenter une abstraction multi-provider (pattern Strategy) avec webhooks signés, idempotence et gestion des méthodes manuelles montre une vraie maîtrise des intégrations de paiement. C'est une compétence transverse très recherchée dans les fintech et les SaaS B2B.

---

