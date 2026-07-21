package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.config.ProvisioningProperties;
import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Premier appel control-plane → backend tenant (docs/16-backlog.md §16.3,
 * fondation partagée items 14/15) — aucune infrastructure de ce genre
 * n'existait avant. Appel en HTTPS public via la Gateway (même chemin que
 * n'importe quel visiteur), pas de résolution cluster-interne : plus simple,
 * aucune CiliumNetworkPolicy à modifier. Authentification par le même secret
 * partagé que le tenant attend sur X-Internal-Api-Key.
 */
@Service
public class TenantStatsClient {

    private static final Logger log = LoggerFactory.getLogger(TenantStatsClient.class);

    public record ResumeTenant(BigDecimal caTotal, long elevesActifs) {
    }

    private final RestClient restClient;
    private final ProvisioningProperties properties;

    public TenantStatsClient(ProvisioningProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    /**
     * {@code Optional.empty()} si le tenant est injoignable ou en échec —
     * un tenant en panne ne doit jamais faire échouer tout le résumé
     * consolidé d'une organisation multi-agences.
     */
    public Optional<ResumeTenant> resumePour(String urlTenant) {
        if (properties.internalStatsApiKey() == null || properties.internalStatsApiKey().isBlank()) {
            log.warn("internal-stats-api-key absent — impossible d'interroger {}", urlTenant);
            return Optional.empty();
        }
        try {
            ResumeTenant resume = restClient.get()
                    .uri("https://{host}/api/stats/resume", urlTenant)
                    .header("X-Internal-Api-Key", properties.internalStatsApiKey())
                    .retrieve()
                    .body(ResumeTenant.class);
            return Optional.ofNullable(resume);
        } catch (RestClientException e) {
            log.warn("Échec de récupération des stats pour {} : {}", urlTenant, e.getMessage());
            return Optional.empty();
        }
    }
}
