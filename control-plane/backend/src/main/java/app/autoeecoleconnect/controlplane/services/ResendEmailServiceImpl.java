package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.config.ProvisioningProperties;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Si {@code app.provisioning.resend-api-key} est vide (dev local / CI),
 * l'envoi est simulé (log uniquement) plutôt que d'échouer.
 */
@Service
public class ResendEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailServiceImpl.class);

    private final RestClient restClient;
    private final ProvisioningProperties properties;

    public ResendEmailServiceImpl(ProvisioningProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + properties.resendApiKey())
                .build();
    }

    @Override
    public void envoyerBienvenue(String destinataire, String nomAutoEcole, String url,
                                  String adminEmail, String adminPassword) {
        String html = """
                <p>Bonjour,</p>
                <p>Votre espace <strong>%s</strong> est prêt : <a href="%s">%s</a></p>
                <p>Identifiants directeur initiaux :</p>
                <ul><li>Email : %s</li><li>Mot de passe : %s</li></ul>
                <p>Pensez à changer ce mot de passe dès votre première connexion.</p>
                """.formatted(nomAutoEcole, url, url, adminEmail, adminPassword);

        envoyer(destinataire, "Votre espace AutoEcoleConnect est prêt", html);
    }

    @Override
    public void envoyerRappelEssai(String destinataire, String nomOrganisation, long joursRestants) {
        String html = """
                <p>Bonjour,</p>
                <p>La période d'essai de <strong>%s</strong> se termine dans <strong>%d jour(s)</strong>.</p>
                <p>Au-delà, l'accès à votre espace sera suspendu — vos données restent
                conservées et l'accès sera rétabli dès l'activation de votre abonnement.</p>
                """.formatted(nomOrganisation, joursRestants);

        envoyer(destinataire, "Votre essai AutoEcoleConnect se termine bientôt", html);
    }

    @Override
    public void envoyerFinEssai(String destinataire, String nomOrganisation) {
        String html = """
                <p>Bonjour,</p>
                <p>La période d'essai de <strong>%s</strong> est terminée : l'accès à votre
                espace est suspendu.</p>
                <p>Vos données sont conservées — contactez-nous pour activer votre
                abonnement et rétablir l'accès.</p>
                """.formatted(nomOrganisation);

        envoyer(destinataire, "Votre essai AutoEcoleConnect est terminé", html);
    }

    @Override
    public void envoyerConfirmationAbonnement(String destinataire, String nomOrganisation, String plan) {
        String html = """
                <p>Bonjour,</p>
                <p>Votre abonnement <strong>%s</strong> pour <strong>%s</strong> est actif :
                votre espace est (ré)ouvert et le restera tant que l'abonnement court.</p>
                <p>Merci de votre confiance !</p>
                """.formatted(plan, nomOrganisation);

        envoyer(destinataire, "Votre abonnement AutoEcoleConnect est actif", html);
    }

    @Override
    public void envoyerEchecPaiement(String destinataire, String nomOrganisation) {
        String html = """
                <p>Bonjour,</p>
                <p>Le dernier paiement de l'abonnement de <strong>%s</strong> a échoué.
                De nouvelles tentatives auront lieu automatiquement — pensez à vérifier
                votre moyen de paiement pour éviter la suspension de votre espace.</p>
                """.formatted(nomOrganisation);

        envoyer(destinataire, "Échec de paiement — action requise", html);
    }

    @Override
    public void envoyerConfirmationSuppression(String destinataire, String nomOrganisation) {
        String html = """
                <p>Bonjour,</p>
                <p>L'espace <strong>%s</strong> et l'ensemble de ses données viennent
                d'être définitivement supprimés, conformément à notre politique de
                rétention (60 jours après suspension).</p>
                <p>Merci d'avoir essayé AutoEcoleConnect.</p>
                """.formatted(nomOrganisation);

        envoyer(destinataire, "Votre espace AutoEcoleConnect a été supprimé", html);
    }

    private void envoyer(String destinataire, String sujet, String html) {
        if (properties.resendApiKey() == null || properties.resendApiKey().isBlank()) {
            log.info("[RESEND_API_KEY absent] Email « {} » simulé pour {} — contenu : {}",
                    sujet, destinataire, html);
            return;
        }

        Map<String, Object> body = Map.of(
                "from", properties.resendFrom(),
                "to", List.of(destinataire),
                "subject", sujet,
                "html", html);

        try {
            restClient.post()
                    .uri("/emails")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // Ne bloque jamais le flux appelant — l'email est une notification,
            // pas une condition de succès (provisioning comme cycle de vie).
            log.warn("Échec envoi email « {} » à {} : HTTP {}", sujet, destinataire, e.getStatusCode());
        }
    }
}
