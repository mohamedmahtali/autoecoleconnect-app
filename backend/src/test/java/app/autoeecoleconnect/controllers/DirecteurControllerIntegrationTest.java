package app.autoeecoleconnect.controllers;

import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.DirecteurCreationRequest;
import app.autoeecoleconnect.controllers.dto.DirecteurResponse;
import app.autoeecoleconnect.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gestion des directeurs (docs/16-backlog.md item 37) : avant la refonte,
 * un tenant n'avait qu'un directeur, créé au démarrage et irremplaçable.
 */
class DirecteurControllerIntegrationTest extends AbstractIntegrationTest {

    private DirecteurCreationRequest requete(String email) {
        return new DirecteurCreationRequest("Martin", "Claire", email, "motdepasse-solide");
    }

    @Test
    void un_directeur_peut_en_creer_un_autre_qui_peut_se_connecter() {
        String email = "directeur-" + UUID.randomUUID() + "@example.fr";

        ResponseEntity<DirecteurResponse> creation =
                rest.postForEntity("/api/directeurs", requete(email), DirecteurResponse.class);

        assertThat(creation.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creation.getBody().email()).isEqualTo(email);
        assertThat(creation.getBody().autoEcoleId()).isNotNull();

        // Le nouveau compte est réellement utilisable, et son jeton porte bien
        // le périmètre de son agence.
        ResponseEntity<LoginResponse> connexion = restAnonyme.postForEntity(
                url("/api/auth/login"),
                new LoginRequest(email, "motdepasse-solide"),
                LoginResponse.class);

        assertThat(connexion.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(connexion.getBody().role()).isEqualTo("DIRECTEUR");
    }

    /**
     * Supprimer un directeur parmi plusieurs est autorisé. Le garde-fou du
     * « dernier directeur » est vérifié dans
     * {@code AutoEcoleIsolationIntegrationTest}, sur une agence dédiée : ici
     * la base est partagée entre classes de test, le nombre de directeurs
     * dépend donc de l'ordre d'exécution.
     */
    @Test
    void un_directeur_parmi_plusieurs_peut_etre_supprime() {
        String email = "temporaire-" + UUID.randomUUID() + "@example.fr";
        UUID cree = rest.postForEntity("/api/directeurs", requete(email), DirecteurResponse.class)
                .getBody().id();

        ResponseEntity<Void> suppression = rest.exchange(
                "/api/directeurs/" + cree, HttpMethod.DELETE, null, Void.class);

        assertThat(suppression.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/directeurs", DirecteurResponse[].class).getBody())
                .extracting(DirecteurResponse::id).doesNotContain(cree);
    }

    @Test
    void un_eleve_authentifie_ne_peut_pas_lister_les_directeurs() {
        String email = "eleve-dir-" + UUID.randomUUID() + "@example.fr";
        rest.postForEntity("/api/clients", new ClientCreationRequest(
                "Petit", "Léa", email, "motdepasse-solide", null, null, null), String.class);

        String jetonEleve = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, "motdepasse-solide"), LoginResponse.class)
                .getBody().token();

        HttpHeaders entetes = new HttpHeaders();
        entetes.setBearerAuth(jetonEleve);
        ResponseEntity<String> reponse = restAnonyme.exchange(
                url("/api/directeurs"), HttpMethod.GET, new HttpEntity<>(entetes), String.class);

        // 403 et non 401 : le jeton est valide, c'est le rôle qui ne suffit pas.
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
