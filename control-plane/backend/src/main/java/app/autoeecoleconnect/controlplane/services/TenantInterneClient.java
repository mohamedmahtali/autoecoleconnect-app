package app.autoeecoleconnect.controlplane.services;

import java.util.Map;
import java.util.UUID;

import app.autoeecoleconnect.controlplane.config.ProvisioningProperties;
import app.autoeecoleconnect.controlplane.exceptions.ProvisioningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Écritures du control-plane dans la base d'un tenant, auxquelles il n'a
 * aucun accès direct (docs/18 §18.3 lots 4 et 5). Même canal que
 * {@link TenantStatsClient} : Service ClusterIP du namespace tenant,
 * authentifié par le secret partagé {@code X-Internal-Api-Key}.
 *
 * <p>⚠️ Ne pas repasser par l'URL publique : Cilium conserve l'identité du
 * pod source même vers l'IP publique du nœud, et Envoy répond alors 403 —
 * piège documenté dans docs/08 §8.5.
 */
@Service
public class TenantInterneClient {

    private static final Logger log = LoggerFactory.getLogger(TenantInterneClient.class);

    public record JetonTenant(String token, String type, UUID organisationId) {
    }

    private final RestClient restClient;
    private final ProvisioningProperties properties;

    public TenantInterneClient(ProvisioningProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    public void creerAutoEcole(String namespaceTenant, String nom, String slug, String adresse) {
        exigerCleConfiguree(namespaceTenant);
        try {
            restClient.post()
                    .uri(urlInterne(namespaceTenant, "auto-ecoles"))
                    .header("X-Internal-Api-Key", properties.internalStatsApiKey())
                    .body(Map.of("nom", nom, "slug", slug,
                            "adresse", adresse == null ? "" : adresse))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ProvisioningException(
                    "Échec de création de l'agence %s dans le tenant %s".formatted(slug, namespaceTenant), e);
        }
    }

    /**
     * Demande au tenant un jeton pour le gérant, que le control-plane vient
     * d'authentifier (docs/18 §18.3 lot 5). C'est le tenant qui l'émet, avec
     * sa propre clé de signature : le control-plane n'a pas à la connaître, et
     * aucune clé n'est partagée entre les deux mondes.
     */
    public JetonTenant jetonAcces(String namespaceTenant, String email, String nomComplet,
                                  UUID autoEcoleId) {
        exigerCleConfiguree(namespaceTenant);
        try {
            return restClient.post()
                    .uri(urlInterne(namespaceTenant, "jeton-acces"))
                    .header("X-Internal-Api-Key", properties.internalStatsApiKey())
                    .body(Map.of("email", email, "nomComplet", nomComplet,
                            "autoEcoleId", autoEcoleId.toString()))
                    .retrieve()
                    .body(JetonTenant.class);
        } catch (RestClientException e) {
            throw new ProvisioningException(
                    "Échec d'obtention d'un jeton pour %s sur %s".formatted(email, namespaceTenant), e);
        }
    }

    private String urlInterne(String namespaceTenant, String chemin) {
        return "http://backend.%s.svc.cluster.local:8080/api/internal/%s"
                .formatted(namespaceTenant, chemin);
    }

    private void exigerCleConfiguree(String namespaceTenant) {
        if (properties.internalStatsApiKey() == null || properties.internalStatsApiKey().isBlank()) {
            log.error("internal-stats-api-key absent — appel interne vers {} impossible", namespaceTenant);
            throw new ProvisioningException(
                    "Clé d'API interne non configurée : appel vers " + namespaceTenant + " impossible", null);
        }
    }
}
