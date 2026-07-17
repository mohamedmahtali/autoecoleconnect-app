package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.config.ProvisioningProperties;
import app.autoeecoleconnect.controlplane.models.ProvisioningLog;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.repositories.ProvisioningLogRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Détecte la fin du provisioning sans dépendre de l'ArgoCD Notifications
 * Engine (trop lourd pour la Slice A) : interroge périodiquement la ressource
 * Application ArgoCD de chaque tenant en attente (voir docs/09 §9.2 étape 6).
 */
@Component
public class ArgoCdSyncPoller {

    private static final Logger log = LoggerFactory.getLogger(ArgoCdSyncPoller.class);
    private static final String GROUP = "argoproj.io";
    private static final String VERSION = "v1alpha1";
    private static final String PLURAL = "applications";

    private final TenantRepository tenantRepository;
    private final ProvisioningLogRepository provisioningLogRepository;
    private final CustomObjectsApi customObjectsApi;
    private final EmailService emailService;
    private final ProvisioningProperties properties;
    private final ObjectMapper objectMapper;

    public ArgoCdSyncPoller(TenantRepository tenantRepository,
                             ProvisioningLogRepository provisioningLogRepository,
                             CustomObjectsApi customObjectsApi,
                             EmailService emailService,
                             ProvisioningProperties properties,
                             ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.provisioningLogRepository = provisioningLogRepository;
        this.customObjectsApi = customObjectsApi;
        this.emailService = emailService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.provisioning.poll-interval-ms}")
    public void verifierTenantsEnAttente() {
        List<Tenant> enAttente = tenantRepository.findByStatut("provisioning");
        for (Tenant tenant : enAttente) {
            try {
                verifierUnTenant(tenant);
            } catch (Exception e) {
                log.error("Erreur inattendue en vérifiant le tenant {}", tenant.getSlug(), e);
            }
        }
    }

    private void verifierUnTenant(Tenant tenant) {
        ArgoAppStatus statut = lireStatutApplication(tenant.getSlug());

        if (statut != null && statut.estSyncEtHealthy()) {
            marquerSucces(tenant);
            return;
        }

        if (LocalDateTime.now().isAfter(tenant.getCreatedAt().plusMinutes(properties.timeoutMinutes()))) {
            marquerEchecTimeout(tenant);
        }
    }

    private ArgoAppStatus lireStatutApplication(String slug) {
        try {
            Object result = customObjectsApi.getNamespacedCustomObject(
                    GROUP, VERSION, properties.argocdNamespace(), PLURAL, "tenant-" + slug)
                    .execute();
            return ArgoAppStatus.depuis(result);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                log.warn("Erreur API ArgoCD pour tenant-{} : HTTP {}", slug, e.getCode());
            }
            return null; // pas encore matérialisée, ou erreur transitoire — on retentera au prochain tick
        }
    }

    private void marquerSucces(Tenant tenant) {
        envoyerEmailBienvenue(tenant);

        tenant.setStatut("trial");
        tenantRepository.save(tenant);

        provisioningLogRepository.findFirstByTenantAndActionOrderByCreatedAtDesc(tenant, "provision")
                .ifPresent(logEntry -> {
                    logEntry.setStatut("success");
                    provisioningLogRepository.save(logEntry);
                });

        log.info("Tenant {} provisionné avec succès (Synced/Healthy)", tenant.getSlug());
    }

    private void marquerEchecTimeout(Tenant tenant) {
        tenant.setStatut("failed");
        tenantRepository.save(tenant);

        provisioningLogRepository.findFirstByTenantAndActionOrderByCreatedAtDesc(tenant, "provision")
                .ifPresent(logEntry -> {
                    logEntry.setStatut("failed");
                    logEntry.setDetail("Timeout : ArgoCD n'a pas atteint Synced/Healthy après "
                            + properties.timeoutMinutes() + " minutes");
                    provisioningLogRepository.save(logEntry);
                });

        log.warn("Tenant {} marqué en échec (timeout de provisioning)", tenant.getSlug());
    }

    @SuppressWarnings("unchecked")
    private void envoyerEmailBienvenue(Tenant tenant) {
        try {
            Map<String, String> config = objectMapper.readValue(tenant.getConfig(), Map.class);
            emailService.envoyerBienvenue(
                    tenant.getOrganisation().getEmailGerant(),
                    tenant.getNom(),
                    "http://" + tenant.getUrl(),
                    config.get("adminEmail"),
                    config.get("adminPassword"));
        } catch (Exception e) {
            log.warn("Impossible d'envoyer l'email de bienvenue pour {}", tenant.getSlug(), e);
        }
    }

    /**
     * Vue minimale du statut d'une ressource Application ArgoCD
     * ({@code status.sync.status} / {@code status.health.status}).
     */
    private record ArgoAppStatus(String syncStatus, String healthStatus) {

        boolean estSyncEtHealthy() {
            return "Synced".equals(syncStatus) && "Healthy".equals(healthStatus);
        }

        @SuppressWarnings("unchecked")
        static ArgoAppStatus depuis(Object rawResult) {
            if (!(rawResult instanceof Map<?, ?> root)) {
                return null;
            }
            Object statusObj = root.get("status");
            if (!(statusObj instanceof Map<?, ?> status)) {
                return null;
            }
            String sync = extraireChamp((Map<String, Object>) status, "sync", "status");
            String health = extraireChamp((Map<String, Object>) status, "health", "status");
            return new ArgoAppStatus(sync, health);
        }

        private static String extraireChamp(Map<String, Object> parent, String cle, String sousChamp) {
            Object section = parent.get(cle);
            if (section instanceof Map<?, ?> map) {
                Object valeur = map.get(sousChamp);
                return valeur != null ? valeur.toString() : null;
            }
            return null;
        }
    }
}
