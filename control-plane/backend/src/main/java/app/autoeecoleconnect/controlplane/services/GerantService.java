package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.controllers.dto.ConsolideResponse;
import app.autoeecoleconnect.controlplane.controllers.dto.MesTenantsResponse;
import app.autoeecoleconnect.controlplane.exceptions.OrganisationIntrouvableException;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import java.math.BigDecimal;
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
    private final TenantStatsClient tenantStatsClient;

    public GerantService(OrganisationRepository organisationRepository,
                         TenantRepository tenantRepository,
                         TenantStatsClient tenantStatsClient) {
        this.organisationRepository = organisationRepository;
        this.tenantRepository = tenantRepository;
        this.tenantStatsClient = tenantStatsClient;
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

    public ConsolideResponse consolidePour(UUID organisationId) {
        List<Tenant> tenants = tenantRepository.findByOrganisationId(organisationId);

        BigDecimal caTotal = BigDecimal.ZERO;
        long elevesActifs = 0;
        int enErreur = 0;
        for (Tenant tenant : tenants) {
            var resume = tenantStatsClient.resumePour(tenant.getNamespace());
            if (resume.isPresent()) {
                caTotal = caTotal.add(resume.get().caTotal());
                elevesActifs += resume.get().elevesActifs();
            } else {
                enErreur++;
            }
        }

        return new ConsolideResponse(caTotal, elevesActifs, tenants.size(), enErreur);
    }
}
