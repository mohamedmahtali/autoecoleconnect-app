package app.autoeecoleconnect.controlplane.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.models.WebhookEvent;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import app.autoeecoleconnect.controlplane.repositories.ProvisioningLogRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import app.autoeecoleconnect.controlplane.repositories.WebhookEventRepository;
import com.stripe.model.Event;
import com.stripe.net.ApiResource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ActivationServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;
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

    private ActivationService activationService;

    private static final UUID ORG_ID = UUID.randomUUID();
    private Organisation organisation;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        activationService = new ActivationService(webhookEventRepository, organisationRepository,
                tenantRepository, provisioningLogRepository, tenantScaleService, emailService);

        organisation = new Organisation();
        organisation.setNom("Auto-École Test");
        organisation.setEmailGerant("gerant@test.fr");
        organisation.setPlan("solo");
        organisation.setTrialEndsAt(LocalDateTime.now().plusDays(10));

        tenant = new Tenant();
        tenant.setOrganisation(organisation);
        tenant.setSlug("auto-ecole-test");
        tenant.setNamespace("auto-ecole-test");
        tenant.setNom("Auto-École Test");
        tenant.setUrl("auto-ecole-test.autoecoleconnect.fr");
    }

    // Le vrai constructEvent parse le JSON avec le même GSON : construire les
    // events de test pareil garantit le même chemin de désérialisation.
    private Event eventDepuisJson(String json) {
        return ApiResource.GSON.fromJson(json, Event.class);
    }

    private Event eventCheckoutComplete() {
        return eventDepuisJson("""
                {"id":"evt_test_1","api_version":"2026-06-24.dahlia","type":"checkout.session.completed","data":{"object":{
                "object":"checkout.session","id":"cs_test_1",
                "client_reference_id":"%s","customer":"cus_test_1","subscription":"sub_test_1"}}}
                """.formatted(ORG_ID));
    }

    @Test
    void checkoutCompleteActiveOrganisationEtTenants() throws Exception {
        tenant.setStatut("suspended");
        tenant.setSuspendedAt(LocalDateTime.now().minusDays(2));
        when(organisationRepository.findById(ORG_ID)).thenReturn(Optional.of(organisation));
        when(tenantRepository.findByOrganisationId(ORG_ID)).thenReturn(List.of(tenant));

        activationService.traiter(eventCheckoutComplete());

        assertThat(organisation.getStatut()).isEqualTo("active");
        assertThat(organisation.getStripeCustomerId()).isEqualTo("cus_test_1");
        assertThat(organisation.getStripeSubscriptionId()).isEqualTo("sub_test_1");
        verify(tenantScaleService).scalerA("auto-ecole-test", 1);
        assertThat(tenant.getStatut()).isEqualTo("active");
        assertThat(tenant.getSuspendedAt()).isNull();
        verify(emailService).envoyerConfirmationAbonnement("gerant@test.fr", "Auto-École Test", "solo");
    }

    @Test
    void eventDejaTraiteEstIgnore() {
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(webhookEventRepository).saveAndFlush(any(WebhookEvent.class));

        activationService.traiter(eventCheckoutComplete());

        verify(organisationRepository, never()).findById(any());
        verify(emailService, never()).envoyerConfirmationAbonnement(anyString(), anyString(), anyString());
    }

    @Test
    void subscriptionDeletedSuspendOrganisationEtTenants() throws Exception {
        organisation.setStatut("active");
        tenant.setStatut("active");
        when(organisationRepository.findById(ORG_ID)).thenReturn(Optional.of(organisation));
        // any() : l'id de l'entité (généré par JPA) est null en test unitaire
        when(tenantRepository.findByOrganisationId(any())).thenReturn(List.of(tenant));

        Event event = eventDepuisJson("""
                {"id":"evt_test_2","api_version":"2026-06-24.dahlia","type":"customer.subscription.deleted","data":{"object":{
                "object":"subscription","id":"sub_test_1","customer":"cus_test_1",
                "metadata":{"organisation_id":"%s"}}}}
                """.formatted(ORG_ID));
        activationService.traiter(event);

        assertThat(organisation.getStatut()).isEqualTo("suspended");
        assertThat(organisation.getStripeSubscriptionId()).isNull();
        verify(tenantScaleService).scalerAZero("auto-ecole-test");
        assertThat(tenant.getStatut()).isEqualTo("suspended");
        assertThat(tenant.getSuspendedAt()).isNotNull();
    }

    @Test
    void paymentFailedMarqueLaDate() {
        organisation.setStatut("active");
        when(organisationRepository.findByStripeCustomerId("cus_test_1"))
                .thenReturn(Optional.of(organisation));

        Event event = eventDepuisJson("""
                {"id":"evt_test_3","api_version":"2026-06-24.dahlia","type":"invoice.payment_failed","data":{"object":{
                "object":"invoice","id":"in_test_1","customer":"cus_test_1"}}}
                """);
        activationService.traiter(event);

        assertThat(organisation.getPaymentFailedAt()).isNotNull();
        verify(emailService).envoyerEchecPaiement("gerant@test.fr", "Auto-École Test");
    }

    @Test
    void typeInconnuEstIgnoreSansErreur() throws Exception {
        Event event = eventDepuisJson(
                """
                {"id":"evt_test_4","api_version":"2026-06-24.dahlia","type":"customer.created","data":{"object":{
                "object":"customer","id":"cus_test_9"}}}
                """);

        activationService.traiter(event);

        verify(tenantScaleService, never()).scalerA(anyString(), anyInt());
        verify(organisationRepository, never()).save(any());
    }
}
