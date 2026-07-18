package app.autoeecoleconnect.controlplane.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import app.autoeecoleconnect.controlplane.AbstractIntegrationTest;
import app.autoeecoleconnect.controlplane.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "gerant-login@test.fr";
    private static final String MOT_DE_PASSE = "Secret123!";

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void creerOrganisation() {
        if (organisationRepository.existsByEmailGerant(EMAIL)) {
            return;
        }
        Organisation organisation = new Organisation();
        organisation.setNom("Auto-École Login");
        organisation.setEmailGerant(EMAIL);
        organisation.setTrialEndsAt(LocalDateTime.now().plusDays(30));
        organisation.setMotDePasseHash(passwordEncoder.encode(MOT_DE_PASSE));
        organisationRepository.save(organisation);
    }

    @Test
    void loginPuisDashboardFonctionnent() {
        ResponseEntity<LoginResponse> login = rest.postForEntity(
                url("/api/auth/login"), new LoginRequest(EMAIL, MOT_DE_PASSE), LoginResponse.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().token()).isNotBlank();
        assertThat(login.getBody().nomOrganisation()).isEqualTo("Auto-École Login");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.getBody().token());
        ResponseEntity<String> dashboard = rest.exchange(
                url("/api/mes-tenants"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboard.getBody()).contains("Auto-École Login");
    }

    @Test
    void loginAvecMauvaisMotDePasseRenvoie401() {
        ResponseEntity<String> reponse = rest.postForEntity(
                url("/api/auth/login"), new LoginRequest(EMAIL, "mauvais-mdp"), String.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void dashboardSansTokenRenvoie401() {
        ResponseEntity<String> reponse = rest.getForEntity(url("/api/mes-tenants"), String.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
