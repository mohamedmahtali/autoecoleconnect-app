package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.config.StripeProperties;
import app.autoeecoleconnect.controlplane.exceptions.AbonnementImpossibleException;
import app.autoeecoleconnect.controlplane.exceptions.OrganisationIntrouvableException;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Création des sessions Stripe Checkout (mode subscription, page hébergée) —
 * docs/05 §5.5. L'organisation est portée deux fois : client_reference_id sur
 * la Session (lu au checkout.session.completed) et metadata sur la
 * Subscription (lue au customer.subscription.deleted).
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final StripeClient stripeClient;
    private final StripeProperties properties;
    private final OrganisationRepository organisationRepository;

    public CheckoutService(StripeClient stripeClient,
                           StripeProperties properties,
                           OrganisationRepository organisationRepository) {
        this.stripeClient = stripeClient;
        this.properties = properties;
        this.organisationRepository = organisationRepository;
    }

    public String creerSessionCheckout(UUID organisationId) {
        if (!properties.estConfigure()) {
            throw new AbonnementImpossibleException(
                    "Le paiement n'est pas encore configuré sur cette instance");
        }

        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(OrganisationIntrouvableException::new);

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceIdPour(organisation.getPlan()))
                        .setQuantity(1L)
                        .build())
                .setClientReferenceId(organisation.getId().toString())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("organisation_id", organisation.getId().toString())
                        .build())
                .setSuccessUrl(properties.baseUrl() + "/dashboard.html?checkout=success")
                .setCancelUrl(properties.baseUrl() + "/dashboard.html?checkout=cancel");

        // Réactivation : réutiliser le Customer existant, sinon Stripe en crée
        // un doublon à chaque checkout. Jamais customer ET customerEmail ensemble.
        if (organisation.getStripeCustomerId() != null) {
            builder.setCustomer(organisation.getStripeCustomerId());
        } else {
            builder.setCustomerEmail(organisation.getEmailGerant());
        }

        try {
            String url = stripeClient.v1().checkout().sessions().create(builder.build()).getUrl();
            log.info("Session Checkout créée pour l'organisation {} (plan {})",
                    organisation.getNom(), organisation.getPlan());
            return url;
        } catch (StripeException e) {
            log.error("Échec de création de la session Checkout pour {}", organisation.getNom(), e);
            throw new AbonnementImpossibleException("Impossible de démarrer le paiement, réessayez");
        }
    }

    private String priceIdPour(String plan) {
        return switch (plan) {
            case "solo" -> properties.priceSolo();
            case "pro" -> properties.pricePro();
            case "groupe" -> properties.priceGroupe();
            default -> throw new AbonnementImpossibleException(
                    "Le plan « " + plan + " » est sur devis — contactez-nous");
        };
    }
}
