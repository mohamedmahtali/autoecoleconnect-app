package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.config.ProvisioningProperties;
import app.autoeecoleconnect.controlplane.controllers.dto.InscriptionRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.InscriptionResponse;
import app.autoeecoleconnect.controlplane.exceptions.EmailGerantDejaUtiliseException;
import app.autoeecoleconnect.controlplane.exceptions.ProvisioningException;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.models.ProvisioningLog;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.ProvisioningLogRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Service
public class ProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(ProvisioningService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganisationRepository organisationRepository;
    private final TenantRepository tenantRepository;
    private final ProvisioningLogRepository provisioningLogRepository;
    private final SlugService slugService;
    private final GitHubService gitHubService;
    private final ProvisioningProperties properties;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final Yaml yaml;

    public ProvisioningService(OrganisationRepository organisationRepository,
                                TenantRepository tenantRepository,
                                ProvisioningLogRepository provisioningLogRepository,
                                SlugService slugService,
                                GitHubService gitHubService,
                                ProvisioningProperties properties,
                                ObjectMapper objectMapper,
                                PasswordEncoder passwordEncoder) {
        this.organisationRepository = organisationRepository;
        this.tenantRepository = tenantRepository;
        this.provisioningLogRepository = provisioningLogRepository;
        this.slugService = slugService;
        this.gitHubService = gitHubService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
    }

    // Pas de @Transactional englobant ici, délibérément : chaque save/saveAndFlush
    // Spring Data s'exécute dans sa propre transaction courte. C'est nécessaire
    // pour que la retry-loop de creerTenantAvecSlugUnique fonctionne — Postgres
    // avorte intégralement une transaction dès la première violation de
    // contrainte, donc un retry dans la MÊME transaction échouerait aussi. Et
    // de toute façon l'appel GitHub externe ne peut pas participer à une
    // transaction DB.
    public InscriptionResponse inscrire(InscriptionRequest request) {
        if (organisationRepository.existsByEmailGerant(request.emailGerant())) {
            throw new EmailGerantDejaUtiliseException(request.emailGerant());
        }

        Organisation organisation = new Organisation();
        organisation.setNom(request.nomAutoEcole());
        organisation.setEmailGerant(request.emailGerant());
        organisation.setPlan(request.plan());
        organisation.setTrialEndsAt(LocalDateTime.now().plusDays(properties.trialDays()));
        // Slice B : mot de passe choisi par le gérant à l'inscription — sert
        // au login Control Plane (dashboard), pas au compte directeur du tenant.
        organisation.setMotDePasseHash(passwordEncoder.encode(request.motDePasse()));
        organisation = organisationRepository.save(organisation);

        String jwtSecret = genererSecretAleatoire();
        String adminPassword = genererMotDePasseAleatoire();

        Tenant tenant = creerTenantAvecSlugUnique(organisation, request, adminPassword);

        ProvisioningLog provisioningLog = new ProvisioningLog(tenant, "provision", "pending", null);
        provisioningLogRepository.save(provisioningLog);

        String valuesYaml = genererValuesYaml(tenant, request.emailGerant(), jwtSecret, adminPassword);

        try {
            gitHubService.commitTenantValues(tenant.getSlug(), valuesYaml);
        } catch (ProvisioningException e) {
            log.error("Provisioning échoué pour {} lors du commit GitHub", tenant.getSlug(), e);
            tenant.setStatut("failed");
            provisioningLog.setStatut("failed");
            provisioningLog.setDetail(e.getMessage());
            tenantRepository.save(tenant);
            provisioningLogRepository.save(provisioningLog);
        }

        return new InscriptionResponse(tenant.getId(), tenant.getSlug(), tenant.getStatut(), tenant.getUrl());
    }

    // Deux inscriptions concurrentes avec un nom identique peuvent toutes deux
    // passer la pré-vérification d'unicité avant que l'une des deux ne commit —
    // un seul retry suffit puisque le second essai porte un suffixe différent.
    private Tenant creerTenantAvecSlugUnique(Organisation organisation, InscriptionRequest request,
                                              String adminPassword) {
        for (int tentative = 0; tentative < 2; tentative++) {
            String slug = slugService.genererSlugUnique(request.nomAutoEcole());
            Tenant tenant = new Tenant();
            tenant.setOrganisation(organisation);
            tenant.setSlug(slug);
            tenant.setNamespace(slug);
            tenant.setNom(request.nomAutoEcole());
            tenant.setUrl(slug + "." + properties.domaine());
            tenant.setPlan(request.plan());
            // Nécessaire au job de polling ArgoCD pour composer l'email de
            // bienvenue une fois le tenant Synced/Healthy (voir ArgoCdSyncPoller).
            tenant.setConfig(construireConfigJson(request.emailGerant(), adminPassword));
            try {
                return tenantRepository.saveAndFlush(tenant);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("Collision de slug détectée pour {} — nouvelle tentative", slug);
            }
        }
        throw new ProvisioningException("Impossible de générer un slug unique pour "
                + request.nomAutoEcole(), null);
    }

    private String construireConfigJson(String adminEmail, String adminPassword) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "adminEmail", adminEmail,
                    "adminPassword", adminPassword));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation JSON impossible", e);
        }
    }

    private String genererValuesYaml(Tenant tenant, String emailGerant,
                                      String jwtSecret, String adminPassword) {
        Map<String, Object> valeurs = new LinkedHashMap<>();

        Map<String, Object> tenantSection = new LinkedHashMap<>();
        tenantSection.put("slug", tenant.getSlug());
        tenantSection.put("nom", tenant.getNom());
        tenantSection.put("plan", tenant.getPlan());
        tenantSection.put("trial", true);
        valeurs.put("tenant", tenantSection);

        Map<String, Object> secrets = new LinkedHashMap<>();
        secrets.put("jwtSecret", jwtSecret);
        secrets.put("adminEmail", emailGerant);
        secrets.put("adminPassword", adminPassword);
        valeurs.put("secrets", secrets);

        Map<String, Object> postgresql = new LinkedHashMap<>();
        postgresql.put("storageSize", "5Gi");
        valeurs.put("postgresql", postgresql);

        return yaml.dump(valeurs);
    }

    private String genererSecretAleatoire() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String genererMotDePasseAleatoire() {
        byte[] bytes = new byte[9];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
