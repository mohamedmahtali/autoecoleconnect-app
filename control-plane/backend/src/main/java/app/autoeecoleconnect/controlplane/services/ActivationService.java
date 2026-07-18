package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.models.ProvisioningLog;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.models.WebhookEvent;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.ProvisioningLogRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import app.autoeecoleconnect.controlplane.repositories.WebhookEventRepository;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import io.kubernetes.client.openapi.ApiException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traitement des webhooks billing (docs/09 §9.5-9.6) : activation/réactivation
 * au paiement, marquage des échecs, suspension à l'annulation d'abonnement.
 *
 * <p>Idempotence : Stripe livre au-moins-une-fois — l'INSERT de l'event dans
 * webhook_events ouvre la transaction ; s'il viole la contrainte unique,
 * l'event a déjà été traité (no-op, 200). Une exception pendant le traitement
 * annule aussi l'INSERT, donc la relivraison Stripe retentera proprement.</p>
 */
@Service
public class ActivationService {

    private static final Logger log = LoggerFactory.getLogger(ActivationService.class);

    private final WebhookEventRepository webhookEventRepository;
    private final OrganisationRepository organisationRepository;
    private final TenantRepository tenantRepository;
    private final ProvisioningLogRepository provisioningLogRepository;
    private final TenantScaleService tenantScaleService;
    private final EmailService emailService;

    public ActivationService(WebhookEventRepository webhookEventRepository,
                             OrganisationRepository organisationRepository,
                             TenantRepository tenantRepository,
                             ProvisioningLogRepository provisioningLogRepository,
                             TenantScaleService tenantScaleService,
                             EmailService emailService) {
        this.webhookEventRepository = webhookEventRepository;
        this.organisationRepository = organisationRepository;
        this.tenantRepository = tenantRepository;
        this.provisioningLogRepository = provisioningLogRepository;
        this.tenantScaleService = tenantScaleService;
        this.emailService = emailService;
    }

    @Transactional
    public void traiter(Event event) {
        try {
            webhookEventRepository.saveAndFlush(new WebhookEvent(event.getId(), event.getType()));
        } catch (DataIntegrityViolationException e) {
            log.info("Webhook {} déjà traité — ignoré (idempotence)", event.getId());
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> activerAbonnement(event);
            case "invoice.payment_failed" -> marquerEchecPaiement(event);
            case "customer.subscription.deleted" -> suspendreApresAnnulation(event);
            default -> log.info("Webhook Stripe {} de type {} ignoré", event.getId(), event.getType());
        }
    }

    private void activerAbonnement(Event event) {
        Session session = (Session) deserialiser(event);
        UUID organisationId = UUID.fromString(session.getClientReferenceId());
        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Organisation inconnue dans checkout.session.completed : " + organisationId));

        organisation.setStripeCustomerId(session.getCustomer());
        organisation.setStripeSubscriptionId(session.getSubscription());
        organisation.setStatut("active");
        organisation.setPaymentFailedAt(null);
        organisationRepository.save(organisation);

        for (Tenant tenant : tenantRepository.findByOrganisationId(organisationId)) {
            if (!"trial".equals(tenant.getStatut()) && !"suspended".equals(tenant.getStatut())) {
                continue;
            }
            reactiverTenant(tenant);
        }

        envoyerSansBloquer(() -> emailService.envoyerConfirmationAbonnement(
                organisation.getEmailGerant(), organisation.getNom(), organisation.getPlan()));
        log.info("Abonnement activé pour {} (plan {})", organisation.getNom(), organisation.getPlan());
    }

    private void reactiverTenant(Tenant tenant) {
        try {
            tenantScaleService.scalerA(tenant.getNamespace(), 1);
        } catch (ApiException e) {
            // Fait échouer la transaction → l'INSERT webhook_events est annulé
            // et Stripe relivrera l'event : la réactivation sera retentée.
            throw new IllegalStateException("Échec du scale-up du tenant " + tenant.getSlug()
                    + " (HTTP " + e.getCode() + ")", e);
        }
        tenant.setStatut("active");
        tenant.setSuspendedAt(null);
        tenantRepository.save(tenant);
        provisioningLogRepository.save(new ProvisioningLog(tenant, "resume", "success",
                "Abonnement Stripe actif"));
    }

    private void marquerEchecPaiement(Event event) {
        Invoice invoice = (Invoice) deserialiser(event);
        // Depuis l'API "basil", invoice.subscription n'est plus au niveau
        // racine — on résout l'organisation par le customer.
        Optional<Organisation> organisation =
                organisationRepository.findByStripeCustomerId(invoice.getCustomer());
        if (organisation.isEmpty()) {
            log.warn("invoice.payment_failed pour un customer inconnu : {}", invoice.getCustomer());
            return;
        }

        Organisation org = organisation.get();
        org.setPaymentFailedAt(LocalDateTime.now());
        organisationRepository.save(org);
        envoyerSansBloquer(() -> emailService.envoyerEchecPaiement(
                org.getEmailGerant(), org.getNom()));
        log.warn("Échec de paiement pour {} — Smart Retries Stripe en cours", org.getNom());
    }

    private void suspendreApresAnnulation(Event event) {
        Subscription subscription = (Subscription) deserialiser(event);
        Organisation organisation = trouverOrganisation(subscription);
        if (organisation == null) {
            log.warn("customer.subscription.deleted sans organisation résoluble (sub {})",
                    subscription.getId());
            return;
        }

        organisation.setStatut("suspended");
        organisation.setStripeSubscriptionId(null);
        organisationRepository.save(organisation);

        for (Tenant tenant : tenantRepository.findByOrganisationId(organisation.getId())) {
            if (!"active".equals(tenant.getStatut()) && !"trial".equals(tenant.getStatut())) {
                continue;
            }
            try {
                tenantScaleService.scalerAZero(tenant.getNamespace());
            } catch (ApiException e) {
                throw new IllegalStateException("Échec du scale-to-zero du tenant "
                        + tenant.getSlug() + " (HTTP " + e.getCode() + ")", e);
            }
            tenant.setStatut("suspended");
            tenant.setSuspendedAt(LocalDateTime.now()); // démarre le compteur J+60
            tenantRepository.save(tenant);
            provisioningLogRepository.save(new ProvisioningLog(tenant, "suspend", "success",
                    "Abonnement Stripe annulé"));
        }

        envoyerSansBloquer(() -> emailService.envoyerFinEssai(
                organisation.getEmailGerant(), organisation.getNom()));
        log.info("Organisation {} suspendue (abonnement annulé)", organisation.getNom());
    }

    private Organisation trouverOrganisation(Subscription subscription) {
        String orgId = subscription.getMetadata() == null
                ? null : subscription.getMetadata().get("organisation_id");
        if (orgId != null) {
            return organisationRepository.findById(UUID.fromString(orgId)).orElse(null);
        }
        return organisationRepository.findByStripeCustomerId(subscription.getCustomer()).orElse(null);
    }

    // deserializeUnsafe : tolère un léger écart de version d'API Stripe (le
    // payload reste structurellement compatible pour les champs qu'on lit).
    private StripeObject deserialiser(Event event) {
        Optional<StripeObject> objet = event.getDataObjectDeserializer().getObject();
        if (objet.isPresent()) {
            return objet.get();
        }
        try {
            log.warn("Désérialisation stricte impossible pour {} (version API {}) — fallback unsafe",
                    event.getId(), event.getApiVersion());
            return event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (com.stripe.exception.EventDataObjectDeserializationException e) {
            throw new IllegalStateException("Payload Stripe illisible pour " + event.getId(), e);
        }
    }

    // L'email est une notification, jamais une condition de succès du webhook.
    private void envoyerSansBloquer(Runnable envoi) {
        try {
            envoi.run();
        } catch (Exception e) {
            log.warn("Échec d'envoi d'email de notification billing", e);
        }
    }
}
