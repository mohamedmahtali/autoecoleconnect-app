package app.autoeecoleconnect.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import app.autoeecoleconnect.controllers.dto.DonneesPersonnellesResponse;
import app.autoeecoleconnect.controllers.dto.ForfaitRequest;
import app.autoeecoleconnect.controllers.dto.ForfaitResponse;
import app.autoeecoleconnect.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controllers.dto.ReservationCreationRequest;
import app.autoeecoleconnect.controllers.dto.ReservationResponse;
import app.autoeecoleconnect.models.CarburantForfait;
import app.autoeecoleconnect.models.CategorieForfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.models.UniteValidite;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class EleveControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String MOT_DE_PASSE = "motdepasse-solide";

    private UUID creerClient(String email) {
        ResponseEntity<ClientResponse> cree = rest.postForEntity("/api/clients",
                new ClientCreationRequest("Durand", "Marie", email, MOT_DE_PASSE,
                        "0600000000", "3 rue des Écoles", null),
                ClientResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return cree.getBody().id();
    }

    private UUID creerForfait() {
        ResponseEntity<ForfaitResponse> cree = rest.postForEntity("/api/forfaits",
                new ForfaitRequest("Forfait export 20h", 20, 6, UniteValidite.MOIS,
                        new BigDecimal("890.00"), null, CategorieForfait.CONDUITE, null,
                        Kilometrage.ILLIMITE, null, CarburantForfait.INCLUS),
                ForfaitResponse.class);
        return cree.getBody().id();
    }

    private HttpHeaders connecter(String email) {
        LoginResponse login = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, MOT_DE_PASSE), LoginResponse.class).getBody();
        HttpHeaders entetes = new HttpHeaders();
        entetes.setBearerAuth(login.token());
        return entetes;
    }

    @Test
    void un_eleve_exporte_ses_propres_donnees() {
        UUID clientId = creerClient("marie.durand.export@example.fr");
        UUID forfaitId = creerForfait();
        rest.postForEntity("/api/reservations",
                new ReservationCreationRequest(clientId, forfaitId,
                        LocalDate.of(2026, 8, 1), null, null, null),
                ReservationResponse.class);

        ResponseEntity<DonneesPersonnellesResponse> export = restAnonyme.exchange(
                url("/api/eleve/mes-donnees"), HttpMethod.GET,
                new HttpEntity<>(connecter("marie.durand.export@example.fr")),
                DonneesPersonnellesResponse.class);

        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getBody().identite().id()).isEqualTo(clientId);
        assertThat(export.getBody().identite().email()).isEqualTo("marie.durand.export@example.fr");
        assertThat(export.getBody().reservations()).hasSize(1);
        assertThat(export.getBody().reservations().get(0).clientNomComplet()).isEqualTo("Marie Durand");
    }

    @Test
    void un_directeur_n_a_pas_acces_a_l_export_eleve() {
        // rest est authentifié en DIRECTEUR : la route est réservée au CLIENT.
        ResponseEntity<String> reponse = rest.getForEntity("/api/eleve/mes-donnees", String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sans_jeton_l_export_est_refuse() {
        ResponseEntity<String> reponse = restAnonyme.getForEntity(
                url("/api/eleve/mes-donnees"), String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
