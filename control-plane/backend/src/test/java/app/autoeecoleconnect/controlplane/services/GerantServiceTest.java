package app.autoeecoleconnect.controlplane.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import app.autoeecoleconnect.controlplane.controllers.dto.ConsolideResponse;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GerantServiceTest {

    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantStatsClient tenantStatsClient;

    private GerantService service;

    private Tenant tenantAvecUrl(String url) {
        Tenant t = new Tenant();
        t.setSlug(url.split("\\.")[0]);
        t.setNamespace(t.getSlug());
        t.setNom(t.getSlug());
        t.setUrl(url);
        return t;
    }

    @Test
    void consolideSommeLesTenantsJoignables() {
        service = new GerantService(organisationRepository, tenantRepository, tenantStatsClient);
        UUID orgId = UUID.randomUUID();
        Tenant a = tenantAvecUrl("agence-a.autoecoleconnect.fr");
        Tenant b = tenantAvecUrl("agence-b.autoecoleconnect.fr");
        when(tenantRepository.findByOrganisationId(orgId)).thenReturn(List.of(a, b));
        when(tenantStatsClient.resumePour("agence-a.autoecoleconnect.fr"))
                .thenReturn(Optional.of(new TenantStatsClient.ResumeTenant(new BigDecimal("500.00"), 3)));
        when(tenantStatsClient.resumePour("agence-b.autoecoleconnect.fr"))
                .thenReturn(Optional.of(new TenantStatsClient.ResumeTenant(new BigDecimal("300.00"), 2)));

        ConsolideResponse consolide = service.consolidePour(orgId);

        assertThat(consolide.caTotal()).isEqualByComparingTo("800.00");
        assertThat(consolide.elevesActifs()).isEqualTo(5);
        assertThat(consolide.tenantsInterroges()).isEqualTo(2);
        assertThat(consolide.tenantsEnErreur()).isEqualTo(0);
    }

    @Test
    void consolideToleresUnTenantInjoignableSansFaireEchouerLeReste() {
        service = new GerantService(organisationRepository, tenantRepository, tenantStatsClient);
        UUID orgId = UUID.randomUUID();
        Tenant ok = tenantAvecUrl("agence-ok.autoecoleconnect.fr");
        Tenant enPanne = tenantAvecUrl("agence-panne.autoecoleconnect.fr");
        when(tenantRepository.findByOrganisationId(orgId)).thenReturn(List.of(ok, enPanne));
        when(tenantStatsClient.resumePour("agence-ok.autoecoleconnect.fr"))
                .thenReturn(Optional.of(new TenantStatsClient.ResumeTenant(new BigDecimal("100.00"), 1)));
        when(tenantStatsClient.resumePour("agence-panne.autoecoleconnect.fr"))
                .thenReturn(Optional.empty());

        ConsolideResponse consolide = service.consolidePour(orgId);

        assertThat(consolide.caTotal()).isEqualByComparingTo("100.00");
        assertThat(consolide.elevesActifs()).isEqualTo(1);
        assertThat(consolide.tenantsInterroges()).isEqualTo(2);
        assertThat(consolide.tenantsEnErreur()).isEqualTo(1);
    }
}
