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
        if (properties.resendApiKey() == null || properties.resendApiKey().isBlank()) {
            log.info("[RESEND_API_KEY absent] Email de bienvenue simulé pour {} ({}) → {} "
                            + "(identifiants directeur : {} / {})",
                    destinataire, nomAutoEcole, url, adminEmail, adminPassword);
            return;
        }

        String html = """
                <p>Bonjour,</p>
                <p>Votre espace <strong>%s</strong> est prêt : <a href="%s">%s</a></p>
                <p>Identifiants directeur initiaux :</p>
                <ul><li>Email : %s</li><li>Mot de passe : %s</li></ul>
                <p>Pensez à changer ce mot de passe dès votre première connexion.</p>
                """.formatted(nomAutoEcole, url, url, adminEmail, adminPassword);

        Map<String, Object> body = Map.of(
                "from", properties.resendFrom(),
                "to", List.of(destinataire),
                "subject", "Votre espace AutoEcoleConnect est prêt",
                "html", html);

        try {
            restClient.post()
                    .uri("/emails")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // Ne bloque jamais le flip de statut trial — l'email est une notification,
            // pas une condition de succès du provisioning.
            log.warn("Échec envoi email de bienvenue à {} : HTTP {}", destinataire, e.getStatusCode());
        }
    }
}
