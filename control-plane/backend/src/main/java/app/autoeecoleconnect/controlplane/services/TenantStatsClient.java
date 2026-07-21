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
 * n'existait avant. Authentification par le même secret partagé que le tenant
 * attend sur X-Internal-Api-Key.
 *
 * <p>Appel <b>cluster-interne</b> (Service ClusterIP du namespace tenant), pas
 * en HTTPS public : le chemin public a été essayé d'abord et ne marche pas.
 * Cilium conserve l'identité du pod source même quand la requête sort vers
 * l'IP publique du nœud, et l'egress du namespace platform n'autorise que
 * l'entité {@code world} sur 443 — l'IP du nœud est l'entité {@code host},
 * donc Envoy (le proxy L7 de Cilium) répond 403 « Access denied ». Le chemin
 * interne demande deux règles CiliumNetworkPolicy explicites (egress côté
 * control-plane, ingress côté tenant, voir les deux charts) mais évite
 * l'aller-retour réseau et la poignée TLS.
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
     * {@code namespaceTenant} est la colonne {@code tenants.namespace} — le
     * Service backend y est toujours nommé « backend » (chart portail-tenant).
     *
     * <p>{@code Optional.empty()} si le tenant est injoignable ou en échec —
     * un tenant en panne ne doit jamais faire échouer tout le résumé
     * consolidé d'une organisation multi-agences.
     */
    public Optional<ResumeTenant> resumePour(String namespaceTenant) {
        if (properties.internalStatsApiKey() == null || properties.internalStatsApiKey().isBlank()) {
            log.warn("internal-stats-api-key absent — impossible d'interroger {}", namespaceTenant);
            return Optional.empty();
        }
        try {
            ResumeTenant resume = restClient.get()
                    .uri("http://backend.{namespace}.svc.cluster.local:8080/api/stats/resume", namespaceTenant)
                    .header("X-Internal-Api-Key", properties.internalStatsApiKey())
                    .retrieve()
                    .body(ResumeTenant.class);
            return Optional.ofNullable(resume);
        } catch (RestClientException e) {
            log.warn("Échec de récupération des stats pour {} : {}", namespaceTenant, e.getMessage());
            return Optional.empty();
        }
    }
}
