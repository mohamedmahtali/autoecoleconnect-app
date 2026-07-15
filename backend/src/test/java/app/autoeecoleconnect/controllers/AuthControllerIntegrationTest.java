package app.autoeecoleconnect.controllers;

import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import app.autoeecoleconnect.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controllers.dto.MoniteurCreationRequest;
import app.autoeecoleconnect.controllers.dto.MoniteurResponse;
import app.autoeecoleconnect.controllers.dto.VoitureRequest;
import app.autoeecoleconnect.models.Transmission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void le_directeur_bootstrap_obtient_un_jwt() {
        ResponseEntity<LoginResponse> reponse = restAnonyme.postForEntity(
                url("/api/auth/login"),
                new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD), LoginResponse.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody().token()).isNotBlank();
        assertThat(reponse.getBody().role()).isEqualTo("DIRECTEUR");
        assertThat(reponse.getBody().type()).isEqualTo("Bearer");
    }

    @Test
    void un_mauvais_mot_de_passe_repond_401_sans_reveler_lemail() {
        ResponseEntity<String> reponse = restAnonyme.postForEntity(
                url("/api/auth/login"),
                new LoginRequest(ADMIN_EMAIL, "mauvais-mot-de-passe"), String.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reponse.getBody()).contains("Email ou mot de passe incorrect");
    }

    @Test
    void sans_token_lapi_repond_401_sauf_le_catalogue_public() {
        assertThat(restAnonyme.getForEntity(url("/api/clients"), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restAnonyme.getForEntity(url("/api/forfaits"), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restAnonyme.getForEntity(url("/api/ping"), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void un_client_peut_lire_mais_pas_ecrire() {
        // Le directeur crée le compte élève
        rest.postForEntity("/api/clients", new ClientCreationRequest(
                        "Faure", "Hugo", "hugo.faure@example.fr", "motdepasse-solide",
                        null, null, null),
                ClientResponse.class);

        // L'élève se connecte
        LoginResponse login = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest("hugo.faure@example.fr", "motdepasse-solide"),
                LoginResponse.class).getBody();
        assertThat(login.role()).isEqualTo("CLIENT");

        HttpHeaders enTetes = new HttpHeaders();
        enTetes.setBearerAuth(login.token());

        // Lecture autorisée
        ResponseEntity<String> lecture = restAnonyme.exchange(url("/api/clients"),
                HttpMethod.GET, new HttpEntity<>(enTetes), String.class);
        assertThat(lecture.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Écriture réservée au directeur
        ResponseEntity<String> ecriture = restAnonyme.exchange(url("/api/voitures"),
                HttpMethod.POST,
                new HttpEntity<>(new VoitureRequest("Twingo", "Renault",
                        Transmission.MANUELLE, false, null, null, null, null, false, null),
                        enTetes),
                String.class);
        assertThat(ecriture.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void un_moniteur_non_approuve_ne_peut_pas_se_connecter() {
        rest.postForEntity("/api/moniteurs", new MoniteurCreationRequest(
                        "Weber", "Sami", "sami.weber@example.fr", "motdepasse-solide",
                        null, null),
                MoniteurResponse.class);

        ResponseEntity<String> reponse = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest("sami.weber@example.fr", "motdepasse-solide"), String.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reponse.getBody()).contains("non approuvé");
    }

    @Test
    void un_token_falsifie_est_rejete() {
        HttpHeaders enTetes = new HttpHeaders();
        enTetes.setBearerAuth("eyJhbGciOiJIUzI1NiJ9." + UUID.randomUUID() + ".signature-bidon");

        ResponseEntity<String> reponse = restAnonyme.exchange(url("/api/clients"),
                HttpMethod.GET, new HttpEntity<>(enTetes), String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
