package app.autoeecoleconnect.controllers;

import java.time.LocalDate;
import java.util.Map;

import app.autoeecoleconnect.controllers.dto.ClientRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration de bout en bout : vrai PostgreSQL (Testcontainers),
 * migrations Liquibase appliquées au démarrage du contexte.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ClientControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestRestTemplate rest;

    @Test
    void cycle_de_vie_complet_dun_client() {
        // Création
        ClientRequest creation = new ClientRequest("Martin", "Lucas",
                "lucas.martin@example.fr", "0698765432", LocalDate.of(2003, 9, 30), null);
        ResponseEntity<ClientResponse> cree =
                rest.postForEntity("/api/clients", creation, ClientResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = cree.getBody().id();
        assertThat(cree.getHeaders().getLocation().getPath()).isEqualTo("/api/clients/" + id);

        // Lecture
        ResponseEntity<ClientResponse> lu =
                rest.getForEntity("/api/clients/" + id, ClientResponse.class);
        assertThat(lu.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lu.getBody().email()).isEqualTo("lucas.martin@example.fr");
        assertThat(lu.getBody().statut().name()).isEqualTo("PROSPECT");

        // Mise à jour
        ClientRequest maj = new ClientRequest("Martin", "Lucas",
                "lucas.martin@example.fr", "0600000000", LocalDate.of(2003, 9, 30), null);
        ResponseEntity<ClientResponse> modifie = rest.exchange("/api/clients/" + id,
                HttpMethod.PUT, new HttpEntity<>(maj), ClientResponse.class);
        assertThat(modifie.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(modifie.getBody().telephone()).isEqualTo("0600000000");

        // Suppression puis 404
        ResponseEntity<Void> supprime = rest.exchange("/api/clients/" + id,
                HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(supprime.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<String> disparu =
                rest.getForEntity("/api/clients/" + id, String.class);
        assertThat(disparu.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void creer_avec_un_email_invalide_repond_400_avec_le_detail() {
        Map<String, String> invalide = Map.of("nom", "Durand", "prenom", "Emma",
                "email", "pas-un-email");
        ResponseEntity<String> reponse =
                rest.postForEntity("/api/clients", invalide, String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).contains("erreurs").contains("email");
    }

    @Test
    void creer_deux_fois_le_meme_email_repond_409() {
        ClientRequest requete = new ClientRequest("Bernard", "Chloé",
                "chloe.bernard@example.fr", null, null, null);
        rest.postForEntity("/api/clients", requete, ClientResponse.class);

        ResponseEntity<String> doublon =
                rest.postForEntity("/api/clients", requete, String.class);
        assertThat(doublon.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
