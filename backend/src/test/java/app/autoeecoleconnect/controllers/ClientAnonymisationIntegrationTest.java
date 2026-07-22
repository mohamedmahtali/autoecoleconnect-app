package app.autoeecoleconnect.controllers;

import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import app.autoeecoleconnect.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ClientAnonymisationIntegrationTest extends AbstractIntegrationTest {

    private static final String MOT_DE_PASSE = "motdepasse-solide";

    private ClientResponse creerClient(String email) {
        ResponseEntity<ClientResponse> cree = rest.postForEntity("/api/clients",
                new ClientCreationRequest("Bernard", "Hugo", email, MOT_DE_PASSE,
                        "0600000000", "5 avenue du Permis", "note interne"),
                ClientResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return cree.getBody();
    }

    @Test
    void anonymiser_efface_les_donnees_et_rend_le_login_impossible() {
        String email = "hugo.bernard.rgpd@example.fr";
        UUID clientId = creerClient(email).id();

        // Le login fonctionne avant l'anonymisation
        assertThat(restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, MOT_DE_PASSE), LoginResponse.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ClientResponse> anonymise = rest.postForEntity(
                "/api/clients/" + clientId + "/anonymisation", null, ClientResponse.class);
        assertThat(anonymise.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(anonymise.getBody().nom()).isEqualTo("[supprimé]");
        assertThat(anonymise.getBody().prenom()).isEqualTo("[supprimé]");
        assertThat(anonymise.getBody().email()).isEqualTo("supprime-" + clientId + "@anonyme.invalid");
        assertThat(anonymise.getBody().telephone()).isNull();
        assertThat(anonymise.getBody().adresse()).isNull();
        assertThat(anonymise.getBody().notes()).isNull();
        assertThat(anonymise.getBody().active()).isFalse();

        // Le client disparaît des listes du directeur
        ResponseEntity<ClientResponse[]> liste = rest.getForEntity("/api/clients", ClientResponse[].class);
        assertThat(liste.getBody()).extracting(ClientResponse::id).doesNotContain(clientId);

        // Login désormais impossible, avec l'ancien comme avec le nouvel email
        assertThat(restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, MOT_DE_PASSE), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anonymiser_un_client_deja_anonymise_repond_404() {
        UUID clientId = creerClient("double.anonymisation@example.fr").id();
        rest.postForEntity("/api/clients/" + clientId + "/anonymisation", null, ClientResponse.class);

        ResponseEntity<String> secondAppel = rest.postForEntity(
                "/api/clients/" + clientId + "/anonymisation", null, String.class);
        assertThat(secondAppel.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void un_eleve_ne_peut_pas_anonymiser() {
        String email = "eleve.sans.droit.anonymisation@example.fr";
        UUID clientId = creerClient(email).id();

        LoginResponse login = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, MOT_DE_PASSE), LoginResponse.class).getBody();
        HttpHeaders entetes = new HttpHeaders();
        entetes.setBearerAuth(login.token());

        ResponseEntity<String> refus = restAnonyme.exchange(
                url("/api/clients/" + clientId + "/anonymisation"), HttpMethod.POST,
                new HttpEntity<>(entetes), String.class);
        assertThat(refus.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
