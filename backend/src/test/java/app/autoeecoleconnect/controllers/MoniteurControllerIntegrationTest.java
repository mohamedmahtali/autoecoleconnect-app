package app.autoeecoleconnect.controllers;

import java.util.Map;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.MoniteurCreationRequest;
import app.autoeecoleconnect.controllers.dto.MoniteurResponse;
import app.autoeecoleconnect.models.StatutMoniteur;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MoniteurControllerIntegrationTest extends AbstractIntegrationTest {

    private UUID creerMoniteur(String email) {
        MoniteurCreationRequest creation = new MoniteurCreationRequest(
                "Benali", "Karim", email, "motdepasse-solide", "0655555555", null);
        ResponseEntity<MoniteurResponse> cree =
                rest.postForEntity("/api/moniteurs", creation, MoniteurResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(cree.getBody().statut()).isEqualTo(StatutMoniteur.PENDING);
        return cree.getBody().id();
    }

    @Test
    void workflow_dapprobation_complet() {
        UUID id = creerMoniteur("karim.benali@example.fr");

        // PENDING → APPROVED
        ResponseEntity<MoniteurResponse> approuve = rest.exchange(
                "/api/moniteurs/" + id + "/statut", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("statut", "APPROVED")), MoniteurResponse.class);
        assertThat(approuve.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approuve.getBody().statut()).isEqualTo(StatutMoniteur.APPROVED);

        // APPROVED → REJECTED est interdit
        ResponseEntity<String> interdit = rest.exchange(
                "/api/moniteurs/" + id + "/statut", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("statut", "REJECTED")), String.class);
        assertThat(interdit.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(interdit.getBody()).contains("Transition de statut interdite");
    }

    @Test
    void rejeter_un_moniteur_pending() {
        UUID id = creerMoniteur("autre.moniteur@example.fr");

        ResponseEntity<MoniteurResponse> rejete = rest.exchange(
                "/api/moniteurs/" + id + "/statut", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("statut", "REJECTED")), MoniteurResponse.class);
        assertThat(rejete.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejete.getBody().statut()).isEqualTo(StatutMoniteur.REJECTED);
    }
}
