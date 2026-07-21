package app.autoeecoleconnect.controlplane.controllers;

import app.autoeecoleconnect.controlplane.AbstractIntegrationTest;
import app.autoeecoleconnect.controlplane.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controlplane.controllers.dto.OrganisationsAdminResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// @DynamicPropertySource calcule le hash BCrypt au lancement plutôt qu'un
// hash pré-calculé en dur dans le code — plus lisible, pas de risque de
// hash/mot de passe désynchronisés.
class AdminControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String SUPERADMIN_EMAIL = "superadmin.test@autoecoleconnect.fr";
    private static final String SUPERADMIN_PASSWORD = "SuperSecret1!";

    @DynamicPropertySource
    static void superadminProperties(DynamicPropertyRegistry registry) {
        registry.add("app.superadmin.email", () -> SUPERADMIN_EMAIL);
        registry.add("app.superadmin.password-hash",
                () -> new BCryptPasswordEncoder().encode(SUPERADMIN_PASSWORD));
    }

    private final TestRestTemplate restAnonyme = new TestRestTemplate();

    @Test
    void un_superadmin_liste_toutes_les_organisations() {
        LoginResponse login = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(SUPERADMIN_EMAIL, SUPERADMIN_PASSWORD), LoginResponse.class).getBody();

        HttpHeaders enTetes = new HttpHeaders();
        enTetes.setBearerAuth(login.token());
        ResponseEntity<OrganisationsAdminResponse> reponse = restAnonyme.exchange(
                url("/api/admin/organisations"), HttpMethod.GET,
                new HttpEntity<>(enTetes), OrganisationsAdminResponse.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().organisations()).isNotNull();
    }

    @Test
    void un_gerant_normal_ne_peut_pas_lister_les_organisations() {
        // rest (non authentifié ici, voir AbstractIntegrationTest du control-plane)
        // sans token du tout : refusé.
        ResponseEntity<String> reponse = restAnonyme.getForEntity(
                url("/api/admin/organisations"), String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
