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

    /**
     * L'URL publique est renseignée mais volontairement différente du
     * namespace : TenantStatsClient appelle le Service cluster-interne
     * (namespace), pas l'URL publique — un stub posé sur l'URL passerait
     * inaperçu si le service se remettait à utiliser getUrl().
     */
    private Tenant tenantNomme(String slug) {
        Tenant t = new Tenant();
        t.setSlug(slug);
        t.setNamespace(slug);
        t.setNom(slug);
        t.setUrl(slug + ".autoecoleconnect.fr");
        return t;
    }

    @Test
    void consolideSommeLesTenantsJoignables() {
        service = new GerantService(organisationRepository, tenantRepository, tenantStatsClient);
        UUID orgId = UUID.randomUUID();
        Tenant a = tenantNomme("agence-a");
        Tenant b = tenantNomme("agence-b");
        when(tenantRepository.findByOrganisationId(orgId)).thenReturn(List.of(a, b));
        when(tenantStatsClient.resumePour("agence-a"))
                .thenReturn(Optional.of(new TenantStatsClient.ResumeTenant(new BigDecimal("500.00"), 3)));
        when(tenantStatsClient.resumePour("agence-b"))
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
        Tenant ok = tenantNomme("agence-ok");
        Tenant enPanne = tenantNomme("agence-panne");
        when(tenantRepository.findByOrganisationId(orgId)).thenReturn(List.of(ok, enPanne));
        when(tenantStatsClient.resumePour("agence-ok"))
                .thenReturn(Optional.of(new TenantStatsClient.ResumeTenant(new BigDecimal("100.00"), 1)));
        when(tenantStatsClient.resumePour("agence-panne"))
                .thenReturn(Optional.empty());

        ConsolideResponse consolide = service.consolidePour(orgId);

        assertThat(consolide.caTotal()).isEqualByComparingTo("100.00");
        assertThat(consolide.elevesActifs()).isEqualTo(1);
        assertThat(consolide.tenantsInterroges()).isEqualTo(2);
        assertThat(consolide.tenantsEnErreur()).isEqualTo(1);
    }
}
