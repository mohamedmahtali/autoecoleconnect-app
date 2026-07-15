package app.autoeecoleconnect.controllers;

import java.util.Map;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientMiseAJourRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ClientControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void cycle_de_vie_complet_dun_client() {
        // Création — le mot de passe ne doit jamais ressortir dans la réponse
        ClientCreationRequest creation = new ClientCreationRequest("Martin", "Lucas",
                "lucas.martin@example.fr", "motdepasse-solide", "0698765432",
                "5 avenue de la République, Lyon", null);
        ResponseEntity<String> creeBrut =
                rest.postForEntity("/api/clients", creation, String.class);
        assertThat(creeBrut.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creeBrut.getBody()).doesNotContain("password", "motdepasse", "$2a$");

        ResponseEntity<ClientResponse> lu = rest.getForEntity(
                creeBrut.getHeaders().getLocation(), ClientResponse.class);
        assertThat(lu.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID id = lu.getBody().id();
        assertThat(lu.getBody().email()).isEqualTo("lucas.martin@example.fr");
        assertThat(lu.getBody().active()).isTrue();

        // Mise à jour
        ClientMiseAJourRequest maj = new ClientMiseAJourRequest("Martin", "Lucas",
                "lucas.martin@example.fr", "0600000000", "5 avenue de la République, Lyon", "RDV lundi");
        ResponseEntity<ClientResponse> modifie = rest.exchange("/api/clients/" + id,
                HttpMethod.PUT, new HttpEntity<>(maj), ClientResponse.class);
        assertThat(modifie.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(modifie.getBody().telephone()).isEqualTo("0600000000");

        // Soft delete puis 404
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
                "email", "pas-un-email", "motDePasse", "motdepasse-solide");
        ResponseEntity<String> reponse =
                rest.postForEntity("/api/clients", invalide, String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).contains("erreurs").contains("email");
    }

    @Test
    void creer_deux_fois_le_meme_email_repond_409() {
        ClientCreationRequest requete = new ClientCreationRequest("Bernard", "Chloé",
                "chloe.bernard@example.fr", "motdepasse-solide", null, null, null);
        rest.postForEntity("/api/clients", requete, ClientResponse.class);

        ResponseEntity<String> doublon =
                rest.postForEntity("/api/clients", requete, String.class);
        assertThat(doublon.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
