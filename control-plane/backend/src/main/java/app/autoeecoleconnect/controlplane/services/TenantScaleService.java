package app.autoeecoleconnect.controlplane.services;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Scale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Scale-to-zero des Deployments d'un tenant (suspension fin d'essai, docs/09
 * §9.4). Lecture/écriture de la sous-ressource scale plutôt qu'un patch : pas
 * d'ambiguïté de content-type, et RBAC limité à deployments (list) +
 * deployments/scale (get/update) — voir helm/portail-tenant/templates/
 * rbac-control-plane.yaml côté infra.
 */
@Service
public class TenantScaleService {

    private static final Logger log = LoggerFactory.getLogger(TenantScaleService.class);

    private final AppsV1Api appsV1Api;

    public TenantScaleService(AppsV1Api appsV1Api) {
        this.appsV1Api = appsV1Api;
    }

    public void scalerAZero(String namespace) throws ApiException {
        scalerA(namespace, 0);
    }

    // Slice C : la réactivation remonte les Deployments à 1 (valeur déclarée
    // par le chart tenant — ArgoCD ignore le drift de replicas, voir
    // applicationset-tenants.yaml ignoreDifferences).
    public void scalerA(String namespace, int replicas) throws ApiException {
        V1DeploymentList deployments = appsV1Api.listNamespacedDeployment(namespace).execute();
        for (V1Deployment deployment : deployments.getItems()) {
            String nom = deployment.getMetadata().getName();
            V1Scale scale = appsV1Api.readNamespacedDeploymentScale(nom, namespace).execute();
            if (scale.getSpec() != null
                    && Integer.valueOf(replicas).equals(scale.getSpec().getReplicas())) {
                continue;
            }
            scale.getSpec().setReplicas(replicas);
            appsV1Api.replaceNamespacedDeploymentScale(nom, namespace, scale).execute();
            log.info("Deployment {}/{} scalé à {} replica(s)", namespace, nom, replicas);
        }
    }
}
