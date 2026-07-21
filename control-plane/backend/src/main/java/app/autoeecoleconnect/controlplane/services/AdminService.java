package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.controllers.dto.OrganisationsAdminResponse;
import app.autoeecoleconnect.controlplane.controllers.dto.OrganisationsAdminResponse.OrganisationResume;
import app.autoeecoleconnect.controlplane.controllers.dto.OrganisationsAdminResponse.TenantResumeAdmin;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Version minimale du super-admin (docs/16-backlog.md §16.3 item 17) : une
 * seule vue en lecture sur toutes les organisations de la plateforme, pas de
 * page web dédiée (choix confirmé) — consultable via Swagger.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final OrganisationRepository organisationRepository;
    private final TenantRepository tenantRepository;

    public AdminService(OrganisationRepository organisationRepository,
                        TenantRepository tenantRepository) {
        this.organisationRepository = organisationRepository;
        this.tenantRepository = tenantRepository;
    }

    public OrganisationsAdminResponse toutesLesOrganisations() {
        var organisations = organisationRepository.findAll().stream()
                .map(org -> new OrganisationResume(
                        org.getId(),
                        org.getNom(),
                        org.getEmailGerant(),
                        org.getStatut(),
                        org.getPlan(),
                        org.getTrialEndsAt(),
                        tenantRepository.findByOrganisationId(org.getId()).stream()
                                .map(t -> new TenantResumeAdmin(t.getSlug(), t.getNom(), t.getUrl(), t.getStatut()))
                                .toList()))
                .toList();
        return new OrganisationsAdminResponse(organisations);
    }
}
