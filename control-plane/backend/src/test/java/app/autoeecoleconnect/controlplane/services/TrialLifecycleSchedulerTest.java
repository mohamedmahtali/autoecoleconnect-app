package app.autoeecoleconnect.controlplane.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.autoeecoleconnect.controlplane.config.LifecycleProperties;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.models.ProvisioningLog;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.ProvisioningLogRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import io.kubernetes.client.openapi.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrialLifecycleSchedulerTest {

    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private ProvisioningLogRepository provisioningLogRepository;
    @Mock
    private TenantScaleService tenantScaleService;
    @Mock
    private EmailService emailService;

    private TrialLifecycleScheduler scheduler;

    private Organisation organisation;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        LifecycleProperties properties =
                new LifecycleProperties("0 0 8 * * *", "0 0 9 * * *", 5L);
        scheduler = new TrialLifecycleScheduler(organisationRepository, tenantRepository,
                provisioningLogRepository, tenantScaleService, emailService, properties);

        organisation = new Organisation();
        organisation.setNom("Auto-École Test");
        organisation.setEmailGerant("gerant@test.fr");
        organisation.setTrialEndsAt(LocalDateTime.now().plusDays(3));

        tenant = new Tenant();
        tenant.setOrganisation(organisation);
        tenant.setSlug("auto-ecole-test");
        tenant.setNamespace("auto-ecole-test");
        tenant.setNom("Auto-École Test");
        tenant.setUrl("auto-ecole-test.autoecoleconnect.fr");
        tenant.setStatut("trial");
    }

    @Test
    void rappelEnvoieEmailEtMarqueReminderSent() {
        when(organisationRepository.findByStatutAndReminderSentFalseAndTrialEndsAtBetween(
                anyString(), any(), any())).thenReturn(List.of(organisation));
        when(tenantRepository.findByOrganisationId(any())).thenReturn(List.of(tenant));

        scheduler.envoyerRappelsFinEssai();

        verify(emailService).envoyerRappelEssai("gerant@test.fr", "Auto-École Test", 2L);
        assertThat(organisation.isReminderSent()).isTrue();
        verify(organisationRepository).save(organisation);

        ArgumentCaptor<ProvisioningLog> logCapture = ArgumentCaptor.forClass(ProvisioningLog.class);
        verify(provisioningLogRepository).save(logCapture.capture());
        assertThat(logCapture.getValue().getAction()).isEqualTo("reminder");
        assertThat(logCapture.getValue().getStatut()).isEqualTo("success");
    }

    @Test
    void suspensionScaleAZeroEtSuspendTenantEtOrganisation() throws ApiException {
        when(organisationRepository.findByStatutAndTrialEndsAtBefore(anyString(), any()))
                .thenReturn(List.of(organisation));
        when(tenantRepository.findByOrganisationId(any())).thenReturn(List.of(tenant));

        scheduler.suspendreEssaisExpires();

        verify(tenantScaleService).scalerAZero("auto-ecole-test");
        assertThat(tenant.getStatut()).isEqualTo("suspended");
        assertThat(tenant.getSuspendedAt()).isNotNull();
        assertThat(organisation.getStatut()).isEqualTo("suspended");
        verify(emailService).envoyerFinEssai("gerant@test.fr", "Auto-École Test");
    }

    @Test
    void suspensionLaisseLOrganisationEnTrialSiLeScaleEchoue() throws ApiException {
        when(organisationRepository.findByStatutAndTrialEndsAtBefore(anyString(), any()))
                .thenReturn(List.of(organisation));
        when(tenantRepository.findByOrganisationId(any())).thenReturn(List.of(tenant));
        doThrow(new ApiException(500, "boom")).when(tenantScaleService).scalerAZero(anyString());

        scheduler.suspendreEssaisExpires();

        // Retentera au prochain tick — pas d'email de fin d'essai prématuré
        assertThat(organisation.getStatut()).isEqualTo("trial");
        verify(emailService, never()).envoyerFinEssai(anyString(), anyString());

        ArgumentCaptor<ProvisioningLog> logCapture = ArgumentCaptor.forClass(ProvisioningLog.class);
        verify(provisioningLogRepository).save(logCapture.capture());
        assertThat(logCapture.getValue().getStatut()).isEqualTo("failed");
    }

    @Test
    void suspensionIgnoreLesTenantsDejaSuspendus() throws ApiException {
        tenant.setStatut("suspended");
        when(organisationRepository.findByStatutAndTrialEndsAtBefore(anyString(), any()))
                .thenReturn(List.of(organisation));
        when(tenantRepository.findByOrganisationId(any())).thenReturn(List.of(tenant));

        scheduler.suspendreEssaisExpires();

        verify(tenantScaleService, never()).scalerAZero(anyString());
        // Tous les tenants déjà traités : l'organisation passe bien en suspended
        assertThat(organisation.getStatut()).isEqualTo("suspended");
    }
}
