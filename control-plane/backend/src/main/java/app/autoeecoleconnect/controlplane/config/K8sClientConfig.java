package app.autoeecoleconnect.controlplane.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client Kubernetes : lecture des Applications ArgoCD (ArgoCdSyncPoller) et
 * scale-to-zero des Deployments tenants (TenantScaleService, Slice B).
 * {@link Config#defaultClient()} détecte automatiquement le ServiceAccount
 * monté dans le pod (token + CA in-cluster) ; en dev local (pas de cluster),
 * il retombe sur un client pointant vers localhost:8080 sans lever d'exception
 * au démarrage — chaque appelant gère ses propres échecs (try/catch), ce qui
 * est acceptable puisqu'aucun cluster n'existe en local.
 */
@Configuration
public class K8sClientConfig {

    private static final Logger log = LoggerFactory.getLogger(K8sClientConfig.class);

    @Bean
    public ApiClient k8sApiClient() {
        try {
            return Config.defaultClient();
        } catch (Exception e) {
            log.warn("Impossible de construire le client Kubernetes ({}) — les appels K8s "
                    + "échoueront tant qu'aucun cluster n'est accessible.", e.getMessage());
            return new ApiClient();
        }
    }

    @Bean
    public CustomObjectsApi customObjectsApi(ApiClient client) {
        return new CustomObjectsApi(client);
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient client) {
        return new AppsV1Api(client);
    }
}
