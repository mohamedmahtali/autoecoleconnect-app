package app.autoeecoleconnect.controlplane.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client Kubernetes utilisé pour lire le statut des ressources Application
 * ArgoCD (voir {@code services.ArgoCdSyncPoller}). {@link Config#defaultClient()}
 * détecte automatiquement le ServiceAccount monté dans le pod (token + CA
 * in-cluster) ; en dev local (pas de cluster), il retombe sur un client
 * pointant vers localhost:8080 sans lever d'exception au démarrage — le
 * poller échouera silencieusement à chaque tentative (voir son propre
 * try/catch), ce qui est acceptable puisqu'aucun ArgoCD n'existe en local.
 */
@Configuration
public class K8sClientConfig {

    private static final Logger log = LoggerFactory.getLogger(K8sClientConfig.class);

    @Bean
    public CustomObjectsApi customObjectsApi() {
        ApiClient client;
        try {
            client = Config.defaultClient();
        } catch (Exception e) {
            log.warn("Impossible de construire le client Kubernetes ({}) — le polling ArgoCD "
                    + "échouera tant qu'aucun cluster n'est accessible.", e.getMessage());
            client = new ApiClient();
        }
        return new CustomObjectsApi(client);
    }
}
