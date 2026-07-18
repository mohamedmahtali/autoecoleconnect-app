package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.controllers.dto.MesTenantsResponse;
import app.autoeecoleconnect.controlplane.exceptions.OrganisationIntrouvableException;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vue lecture seule du gérant sur sa propre organisation (dashboard Slice B).
 */
@Service
@Transactional(readOnly = true)
public class GerantService {

    private final OrganisationRepository organisationRepository;
    private final TenantRepository tenantRepository;

    public GerantService(OrganisationRepository organisationRepository,
                         TenantRepository tenantRepository) {
        this.organisationRepository = organisationRepository;
        this.tenantRepository = tenantRepository;
    }

    public MesTenantsResponse mesTenants(UUID organisationId) {
        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(OrganisationIntrouvableException::new);

        List<MesTenantsResponse.TenantResume> tenants =
                tenantRepository.findByOrganisationId(organisationId).stream()
                        .map(t -> new MesTenantsResponse.TenantResume(
                                t.getSlug(), t.getNom(), t.getUrl(), t.getStatut(),
                                t.getCreatedAt(), t.getSuspendedAt()))
                        .toList();

        return new MesTenantsResponse(
                organisation.getNom(),
                organisation.getStatut(),
                organisation.getPlan(),
                organisation.getTrialEndsAt(),
                tenants);
    }
}
