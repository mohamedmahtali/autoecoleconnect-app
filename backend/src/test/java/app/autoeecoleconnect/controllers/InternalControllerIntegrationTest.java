package app.autoeecoleconnect.controllers;

import java.util.Map;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.AutoEcoleResponse;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Routes internes du tenant (docs/18 §18.3 lots 4 et 5). Elles sont
 * {@code permitAll} côté Spring Security et joignables depuis l'extérieur :
 * le secret partagé est leur seul rempart, ce que ces tests vérifient
 * explicitement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.internal-stats-api-key=cle-interne-de-test-1234")
class InternalControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String CLE = "cle-interne-de-test-1234";

    private HttpEntity<Map<String, String>> avecCle(Map<String, String> corps, String cle) {
        HttpHeaders entetes = new HttpHeaders();
        if (cle != null) {
            entetes.set("X-Internal-Api-Key", cle);
        }
        entetes.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corps, entetes);
    }

    @Test
    void le_control_plane_cree_une_agence_avec_la_cle_interne() {
        String slug = "agence-interne-" + UUID.randomUUID();

        ResponseEntity<AutoEcoleResponse> reponse = restAnonyme.postForEntity(
                url("/api/internal/auto-ecoles"),
                avecCle(Map.of("nom", "Agence Bron", "slug", slug, "adresse", "12 rue de Lyon"), CLE),
                AutoEcoleResponse.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().slug()).isEqualTo(slug);
        assertThat(reponse.getBody().id()).isNotNull();
    }

    /**
     * Idempotence : le control-plane rejoue l'appel après un échec réseau
     * sans créer de doublon — l'agence renvoyée est la même.
     */
    @Test
    void creer_deux_fois_la_meme_agence_ne_cree_pas_de_doublon() {
        String slug = "agence-idem-" + UUID.randomUUID();
        var corps = avecCle(Map.of("nom", "Agence Lyon", "slug", slug, "adresse", ""), CLE);

        UUID premier = restAnonyme.postForEntity(url("/api/internal/auto-ecoles"),
                corps, AutoEcoleResponse.class).getBody().id();
        UUID second = restAnonyme.postForEntity(url("/api/internal/auto-ecoles"),
                corps, AutoEcoleResponse.class).getBody().id();

        assertThat(second).isEqualTo(premier);
    }

    @Test
    void sans_cle_ou_avec_une_mauvaise_cle_la_creation_est_refusee() {
        var corps = Map.of("nom", "Pirate", "slug", "pirate-" + UUID.randomUUID(), "adresse", "");

        assertThat(restAnonyme.postForEntity(url("/api/internal/auto-ecoles"),
                avecCle(corps, null), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(restAnonyme.postForEntity(url("/api/internal/auto-ecoles"),
                avecCle(corps, "mauvaise-cle"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void le_control_plane_obtient_un_jeton_dacces_pour_le_gerant() {
        String slug = "agence-jeton-" + UUID.randomUUID();
        UUID agence = restAnonyme.postForEntity(url("/api/internal/auto-ecoles"),
                avecCle(Map.of("nom", "Agence Jeton", "slug", slug, "adresse", ""), CLE),
                AutoEcoleResponse.class).getBody().id();

        ResponseEntity<LoginResponse> reponse = restAnonyme.postForEntity(
                url("/api/internal/jeton-acces"),
                avecCle(Map.of("email", "gerant@example.fr", "nomComplet", "Groupe Dupont",
                        "autoEcoleId", agence.toString()), CLE),
                LoginResponse.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().role()).isEqualTo("DIRECTEUR");
        assertThat(reponse.getBody().token()).isNotBlank();
    }

    @Test
    void un_jeton_dacces_sur_une_agence_inexistante_est_refuse() {
        ResponseEntity<String> reponse = restAnonyme.postForEntity(
                url("/api/internal/jeton-acces"),
                avecCle(Map.of("email", "gerant@example.fr", "nomComplet", "Groupe",
                        "autoEcoleId", UUID.randomUUID().toString()), CLE),
                String.class);

        // Sans ce contrôle, le gérant recevrait un jeton au périmètre
        // inexistant, donc des listes vides sans explication.
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
