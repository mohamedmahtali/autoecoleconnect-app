package app.autoeecoleconnect.controlplane.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import app.autoeecoleconnect.controlplane.AbstractIntegrationTest;
import app.autoeecoleconnect.controlplane.controllers.dto.InscriptionRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.InscriptionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProvisioningControllerIntegrationTest extends AbstractIntegrationTest {

    private HttpEntity<InscriptionRequest> requeteAvecToken(InscriptionRequest body, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("X-Invite-Token", token);
        }
        return new HttpEntity<>(body, headers);
    }

    @Test
    void inscriptionSansTokenEstRejetee() {
        InscriptionRequest requete = new InscriptionRequest("Auto-École Sans Token", "sanstoken@test.fr", "solo");

        ResponseEntity<String> reponse = rest.postForEntity(
                url("/api/inscription"), requeteAvecToken(requete, null), String.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void inscriptionAvecTokenValideDemarreLeProvisioning() {
        InscriptionRequest requete = new InscriptionRequest("Auto-École Test Marseille", "gerant@marseille.fr", "solo");

        ResponseEntity<InscriptionResponse> reponse = rest.postForEntity(
                url("/api/inscription"), requeteAvecToken(requete, "test-invite-token"),
                InscriptionResponse.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(reponse.getBody().slug()).isEqualTo("auto-ecole-test-marseille");
        assertThat(reponse.getBody().statut()).isEqualTo("provisioning");
    }

    @Test
    void inscriptionAvecEmailDejaUtiliseEstRejetee() {
        InscriptionRequest requete = new InscriptionRequest("Auto-École Doublon", "doublon@test.fr", "solo");
        rest.postForEntity(url("/api/inscription"), requeteAvecToken(requete, "test-invite-token"), String.class);

        ResponseEntity<String> reponse = rest.postForEntity(
                url("/api/inscription"), requeteAvecToken(requete, "test-invite-token"), String.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
