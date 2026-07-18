package app.autoeecoleconnect.controlplane.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import app.autoeecoleconnect.controlplane.config.ProvisioningProperties;
import app.autoeecoleconnect.controlplane.controllers.dto.InscriptionRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.InscriptionResponse;
import app.autoeecoleconnect.controlplane.exceptions.EmailGerantDejaUtiliseException;
import app.autoeecoleconnect.controlplane.exceptions.ProvisioningException;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.ProvisioningLogRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProvisioningServiceTest {

    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private ProvisioningLogRepository provisioningLogRepository;
    @Mock
    private SlugService slugService;
    @Mock
    private GitHubService gitHubService;

    private ProvisioningService provisioningService;

    private static final InscriptionRequest REQUEST =
            new InscriptionRequest("Auto-École Test Marseille", "gerant@marseille.fr", "solo",
                    "MotDePasse123!");

    @BeforeEach
    void setUp() {
        ProvisioningProperties properties = new ProvisioningProperties(
                "167.233.170.196.sslip.io", "mohamedmahtali", "autoecoleconnect-infra",
                "", "", "", "invite-token", 15000L, 15L, 30L, "argocd");
        provisioningService = new ProvisioningService(organisationRepository, tenantRepository,
                provisioningLogRepository, slugService, gitHubService, properties, new ObjectMapper(),
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
    }

    // Pas dans setUp : le test de rejet d'email n'atteint jamais les save —
    // Mockito (strict stubs) considérerait ces stubs comme inutiles.
    private void stubberLesSaves() {
        when(organisationRepository.save(any(Organisation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.saveAndFlush(any(Tenant.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void inscrireCreeUnTenantEnProvisioning() {
        stubberLesSaves();
        when(organisationRepository.existsByEmailGerant(anyString())).thenReturn(false);
        when(slugService.genererSlugUnique(anyString())).thenReturn("auto-ecole-test-marseille");

        InscriptionResponse reponse = provisioningService.inscrire(REQUEST);

        assertThat(reponse.slug()).isEqualTo("auto-ecole-test-marseille");
        assertThat(reponse.statut()).isEqualTo("provisioning");
        assertThat(reponse.url()).isEqualTo("auto-ecole-test-marseille.167.233.170.196.sslip.io");
    }

    @Test
    void inscrireRejetteUnEmailDejaUtilise() {
        when(organisationRepository.existsByEmailGerant("gerant@marseille.fr")).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(EmailGerantDejaUtiliseException.class,
                () -> provisioningService.inscrire(REQUEST));
    }

    @Test
    void inscrireMarqueEnEchecSiLeCommitGitHubEchoue() {
        stubberLesSaves();
        when(organisationRepository.existsByEmailGerant(anyString())).thenReturn(false);
        when(slugService.genererSlugUnique(anyString())).thenReturn("auto-ecole-test-marseille");
        doThrow(new ProvisioningException("boom", new RuntimeException()))
                .when(gitHubService).commitTenantValues(anyString(), anyString());

        InscriptionResponse reponse = provisioningService.inscrire(REQUEST);

        assertThat(reponse.statut()).isEqualTo("failed");
    }
}
