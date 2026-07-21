package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.config.LifecycleProperties;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.models.ProvisioningLog;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.ProvisioningLogRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import io.kubernetes.client.openapi.ApiException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cycle de vie du trial (docs/09 §9.3-9.4), Slice B : rappel avant la fin
 * d'essai puis suspension automatique à l'expiration. Même pattern @Scheduled
 * que {@link ArgoCdSyncPoller} (plutôt que les CronJobs K8s esquissés dans la
 * doc) : pas d'objet K8s supplémentaire, un échec individuel ne bloque pas le
 * lot, et une exécution ratée est rattrapée au tick suivant.
 */
@Component
public class TrialLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrialLifecycleScheduler.class);

    private final OrganisationRepository organisationRepository;
    private final TenantRepository tenantRepository;
    private final ProvisioningLogRepository provisioningLogRepository;
    private final TenantScaleService tenantScaleService;
    private final EmailService emailService;
    private final GitHubService gitHubService;
    private final LifecycleProperties properties;

    public TrialLifecycleScheduler(OrganisationRepository organisationRepository,
                                    TenantRepository tenantRepository,
                                    ProvisioningLogRepository provisioningLogRepository,
                                    TenantScaleService tenantScaleService,
                                    EmailService emailService,
                                    GitHubService gitHubService,
                                    LifecycleProperties properties) {
        this.organisationRepository = organisationRepository;
        this.tenantRepository = tenantRepository;
        this.provisioningLogRepository = provisioningLogRepository;
        this.tenantScaleService = tenantScaleService;
        this.emailService = emailService;
        this.gitHubService = gitHubService;
        this.properties = properties;
    }

    @Scheduled(cron = "${app.lifecycle.reminder-cron}")
    public void envoyerRappelsFinEssai() {
        LocalDateTime maintenant = LocalDateTime.now();
        List<Organisation> aRappeler = organisationRepository
                .findByStatutAndReminderSentFalseAndTrialEndsAtBetween(
                        "trial", maintenant, maintenant.plusDays(properties.reminderJoursAvant()));

        for (Organisation organisation : aRappeler) {
            try {
                rappeler(organisation, maintenant);
            } catch (Exception e) {
                log.error("Échec du rappel de fin d'essai pour {}", organisation.getNom(), e);
            }
        }
    }

    private void rappeler(Organisation organisation, LocalDateTime maintenant) {
        long joursRestants = Math.max(0,
                ChronoUnit.DAYS.between(maintenant, organisation.getTrialEndsAt()));

        emailService.envoyerRappelEssai(
                organisation.getEmailGerant(), organisation.getNom(), joursRestants);

        organisation.setReminderSent(true);
        organisationRepository.save(organisation);

        for (Tenant tenant : tenantRepository.findByOrganisationId(organisation.getId())) {
            provisioningLogRepository.save(new ProvisioningLog(tenant, "reminder", "success",
                    "Rappel fin d'essai envoyé (" + joursRestants + " jour(s) restant(s))"));
        }

        log.info("Rappel de fin d'essai envoyé à {} ({} jour(s) restant(s))",
                organisation.getEmailGerant(), joursRestants);
    }

    // Rappel précoce J-25 (docs/16-backlog.md §16.3 item 16) — même
    // déclencheur horaire que le rappel J-5, fenêtre et flag distincts pour
    // ne pas interférer avec lui.
    @Scheduled(cron = "${app.lifecycle.reminder-cron}")
    public void envoyerRappelsPrecoceFinEssai() {
        LocalDateTime maintenant = LocalDateTime.now();
        List<Organisation> aRappeler = organisationRepository
                .findByStatutAndReminderPrecoceSentFalseAndTrialEndsAtBetween(
                        "trial", maintenant, maintenant.plusDays(properties.reminderPrecoceJoursAvant()));

        for (Organisation organisation : aRappeler) {
            try {
                rappelerPrecoce(organisation, maintenant);
            } catch (Exception e) {
                log.error("Échec du rappel précoce de fin d'essai pour {}", organisation.getNom(), e);
            }
        }
    }

    private void rappelerPrecoce(Organisation organisation, LocalDateTime maintenant) {
        long joursRestants = Math.max(0,
                ChronoUnit.DAYS.between(maintenant, organisation.getTrialEndsAt()));

        emailService.envoyerRappelEssai(
                organisation.getEmailGerant(), organisation.getNom(), joursRestants);

        organisation.setReminderPrecoceSent(true);
        organisationRepository.save(organisation);

        for (Tenant tenant : tenantRepository.findByOrganisationId(organisation.getId())) {
            provisioningLogRepository.save(new ProvisioningLog(tenant, "reminder-precoce", "success",
                    "Rappel précoce fin d'essai envoyé (" + joursRestants + " jour(s) restant(s))"));
        }

        log.info("Rappel précoce de fin d'essai envoyé à {} ({} jour(s) restant(s))",
                organisation.getEmailGerant(), joursRestants);
    }

    @Scheduled(cron = "${app.lifecycle.suspend-cron}")
    public void suspendreEssaisExpires() {
        List<Organisation> expirees = organisationRepository
                .findByStatutAndTrialEndsAtBefore("trial", LocalDateTime.now());

        for (Organisation organisation : expirees) {
            try {
                suspendre(organisation);
            } catch (Exception e) {
                log.error("Échec de la suspension de {}", organisation.getNom(), e);
            }
        }
    }

    private void suspendre(Organisation organisation) {
        boolean toutSuspendu = true;

        for (Tenant tenant : tenantRepository.findByOrganisationId(organisation.getId())) {
            if (!"trial".equals(tenant.getStatut()) && !"active".equals(tenant.getStatut())) {
                continue; // provisioning/failed/suspended/deleted : rien à suspendre
            }
            try {
                tenantScaleService.scalerAZero(tenant.getNamespace());
                tenant.setStatut("suspended");
                tenant.setSuspendedAt(LocalDateTime.now());
                tenantRepository.save(tenant);
                provisioningLogRepository.save(new ProvisioningLog(tenant, "suspend", "success",
                        "Fin de période d'essai"));
                log.info("Tenant {} suspendu (fin d'essai)", tenant.getSlug());
            } catch (ApiException e) {
                // L'organisation reste en trial : nouvel essai au prochain tick,
                // les tenants déjà suspendus seront ignorés par le filtre de statut.
                toutSuspendu = false;
                provisioningLogRepository.save(new ProvisioningLog(tenant, "suspend", "failed",
                        "Erreur API Kubernetes (HTTP " + e.getCode() + ")"));
                log.error("Échec du scale-to-zero pour le tenant {} : HTTP {}",
                        tenant.getSlug(), e.getCode(), e);
            }
        }

        if (toutSuspendu) {
            organisation.setStatut("suspended");
            organisationRepository.save(organisation);
            try {
                emailService.envoyerFinEssai(organisation.getEmailGerant(), organisation.getNom());
            } catch (Exception e) {
                log.warn("Impossible d'envoyer l'email de fin d'essai à {}",
                        organisation.getEmailGerant(), e);
            }
        }
    }

    // Suppression J+60 (docs/09 §9.7, Slice C) : le retrait du values.yaml du
    // repo GitOps déclenche le prune ArgoCD (toutes les ressources du tenant,
    // y compris la base CNPG et ses PVC — données définitivement effacées).
    // Le namespace K8s reste un shell vide, accepté (le purger exigerait un
    // ClusterRole delete namespaces, disproportionné).
    @Scheduled(cron = "${app.lifecycle.delete-cron}")
    public void supprimerComptesExpires() {
        LocalDateTime limite = LocalDateTime.now().minusDays(properties.suppressionJoursApres());
        List<Tenant> aSupprimer = tenantRepository.findByStatutAndSuspendedAtBefore("suspended", limite);

        for (Tenant tenant : aSupprimer) {
            try {
                supprimer(tenant);
            } catch (Exception e) {
                log.error("Échec de la suppression du tenant {}", tenant.getSlug(), e);
            }
        }
    }

    private void supprimer(Tenant tenant) {
        Organisation organisation = tenant.getOrganisation();
        // Sécurité : une organisation redevenue active (réabonnement pendant la
        // fenêtre J+60) ne doit jamais être supprimée.
        if ("active".equals(organisation.getStatut())) {
            return;
        }

        gitHubService.deleteTenantValues(tenant.getSlug());

        tenant.setStatut("deleted");
        tenant.setDeletedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
        provisioningLogRepository.save(new ProvisioningLog(tenant, "delete", "success",
                "Fin de vie J+" + properties.suppressionJoursApres() + " après suspension"));
        log.info("Tenant {} supprimé (GitOps prune déclenché)", tenant.getSlug());

        boolean tousSupprimes = tenantRepository.findByOrganisationId(organisation.getId()).stream()
                .allMatch(t -> "deleted".equals(t.getStatut()));
        if (tousSupprimes) {
            // Email AVANT anonymisation (après, l'adresse n'existe plus).
            try {
                emailService.envoyerConfirmationSuppression(
                        organisation.getEmailGerant(), organisation.getNom());
            } catch (Exception e) {
                log.warn("Impossible d'envoyer la confirmation de suppression", e);
            }
            anonymiser(organisation);
        }
    }

    // RGPD : plus aucune donnée personnelle en base après suppression.
    private void anonymiser(Organisation organisation) {
        organisation.setNom("[supprimé]");
        organisation.setEmailGerant("supprime-" + organisation.getId() + "@anonyme.invalid");
        organisation.setMotDePasseHash(null); // login définitivement impossible
        organisation.setStripeCustomerId(null);
        organisation.setStripeSubscriptionId(null);
        organisation.setStatut("deleted");
        organisationRepository.save(organisation);
        log.info("Organisation {} anonymisée (RGPD)", organisation.getId());
    }
}
