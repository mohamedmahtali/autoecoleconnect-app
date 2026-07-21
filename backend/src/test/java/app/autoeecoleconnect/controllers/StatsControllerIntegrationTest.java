package app.autoeecoleconnect.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import app.autoeecoleconnect.controllers.dto.ForfaitRequest;
import app.autoeecoleconnect.controllers.dto.ForfaitResponse;
import app.autoeecoleconnect.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controllers.dto.MoniteurCreationRequest;
import app.autoeecoleconnect.controllers.dto.MoniteurResponse;
import app.autoeecoleconnect.controllers.dto.PaiementManuelRequest;
import app.autoeecoleconnect.controllers.dto.ReservationCreationRequest;
import app.autoeecoleconnect.controllers.dto.ReservationResponse;
import app.autoeecoleconnect.controllers.dto.StatsResponse;
import app.autoeecoleconnect.models.CarburantForfait;
import app.autoeecoleconnect.models.CategorieForfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.models.PaiementType;
import app.autoeecoleconnect.models.UniteValidite;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

// Propriété dédiée (contexte Spring séparé du reste de la suite, même
// PostgreSQL Testcontainers partagé) : app.internal-stats-api-key est vide
// par défaut ailleurs, il faut une vraie valeur pour tester le chemin
// "control-plane" du contrôleur.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.internal-stats-api-key=test-cle-interne-1234")
class StatsControllerIntegrationTest extends AbstractIntegrationTest {

    private UUID creerClient(String email) {
        return rest.postForEntity("/api/clients",
                new ClientCreationRequest("Petit", "Léa", email, "motdepasse-solide",
                        null, null, null),
                ClientResponse.class).getBody().id();
    }

    private UUID creerForfait() {
        return rest.postForEntity("/api/forfaits",
                new ForfaitRequest("Forfait stats 20h", 20, 6, UniteValidite.MOIS,
                        new BigDecimal("890.00"), null, CategorieForfait.CONDUITE, null,
                        Kilometrage.ILLIMITE, null, CarburantForfait.INCLUS),
                ForfaitResponse.class).getBody().id();
    }

    private HttpHeaders enTetesPour(String email, String motDePasse) {
        LoginResponse login = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, motDePasse), LoginResponse.class).getBody();
        HttpHeaders enTetes = new HttpHeaders();
        enTetes.setBearerAuth(login.token());
        return enTetes;
    }

    @Test
    void un_directeur_voit_le_resume_avec_des_donnees_coherentes() {
        UUID clientId = creerClient("stats.directeur@example.fr");
        UUID forfaitId = creerForfait();
        ReservationResponse reservation = rest.postForEntity("/api/reservations",
                new ReservationCreationRequest(clientId, forfaitId,
                        LocalDate.of(2026, 8, 1), null, null, null),
                ReservationResponse.class).getBody();
        rest.exchange("/api/reservations/" + reservation.id() + "/paiement", HttpMethod.PATCH,
                new HttpEntity<>(new PaiementManuelRequest(PaiementType.ESPECE, null)),
                ReservationResponse.class);

        ResponseEntity<StatsResponse> reponse = rest.getForEntity("/api/stats/resume", StatsResponse.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        StatsResponse stats = reponse.getBody();
        assertThat(stats.caTotal()).isGreaterThanOrEqualTo(new BigDecimal("890.00"));
        assertThat(stats.elevesActifs()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void le_control_plane_lit_le_resume_via_la_cle_interne_sans_jwt() {
        HttpHeaders enTetes = new HttpHeaders();
        enTetes.set("X-Internal-Api-Key", "test-cle-interne-1234");

        ResponseEntity<StatsResponse> reponse = restAnonyme.exchange(url("/api/stats/resume"),
                HttpMethod.GET, new HttpEntity<>(enTetes), StatsResponse.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void une_mauvaise_cle_interne_sans_jwt_est_refusee() {
        HttpHeaders enTetes = new HttpHeaders();
        enTetes.set("X-Internal-Api-Key", "mauvaise-cle");

        ResponseEntity<String> reponse = restAnonyme.exchange(url("/api/stats/resume"),
                HttpMethod.GET, new HttpEntity<>(enTetes), String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void un_moniteur_ne_peut_pas_lire_le_resume() {
        MoniteurResponse moniteur = rest.postForEntity("/api/moniteurs",
                new MoniteurCreationRequest("Garnier", "Paul", "stats.moniteur@example.fr",
                        "motdepasse-solide", null, null),
                MoniteurResponse.class).getBody();
        rest.exchange("/api/moniteurs/" + moniteur.id() + "/statut", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("statut", "APPROVED")), MoniteurResponse.class);

        HttpHeaders enTetesMoniteur = enTetesPour("stats.moniteur@example.fr", "motdepasse-solide");
        ResponseEntity<String> reponse = restAnonyme.exchange(url("/api/stats/resume"),
                HttpMethod.GET, new HttpEntity<>(enTetesMoniteur), String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
