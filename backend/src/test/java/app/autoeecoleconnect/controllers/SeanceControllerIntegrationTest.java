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
import app.autoeecoleconnect.controllers.dto.ReservationCreationRequest;
import app.autoeecoleconnect.controllers.dto.ReservationResponse;
import app.autoeecoleconnect.controllers.dto.SeanceCreationRequest;
import app.autoeecoleconnect.controllers.dto.SeanceResponse;
import app.autoeecoleconnect.controllers.dto.VoitureRequest;
import app.autoeecoleconnect.controllers.dto.VoitureResponse;
import app.autoeecoleconnect.models.CarburantForfait;
import app.autoeecoleconnect.models.CategorieForfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.models.StatutSeance;
import app.autoeecoleconnect.models.Transmission;
import app.autoeecoleconnect.models.UniteValidite;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SeanceControllerIntegrationTest extends AbstractIntegrationTest {

    private UUID creerReservation(String emailClient) {
        ClientResponse client = rest.postForEntity("/api/clients",
                new ClientCreationRequest("Roux", "Nina", emailClient,
                        "motdepasse-solide", null, null, null),
                ClientResponse.class).getBody();
        ForfaitResponse forfait = rest.postForEntity("/api/forfaits",
                new ForfaitRequest("Forfait séance 20h", 20, 6, UniteValidite.MOIS,
                        new BigDecimal("890.00"), null, CategorieForfait.CONDUITE, null,
                        Kilometrage.ILLIMITE, null, CarburantForfait.INCLUS),
                ForfaitResponse.class).getBody();
        ReservationResponse reservation = rest.postForEntity("/api/reservations",
                new ReservationCreationRequest(client.id(), forfait.id(),
                        LocalDate.of(2026, 8, 1), null, null, null),
                ReservationResponse.class).getBody();
        return reservation.id();
    }

    private UUID creerMoniteurApprouve(String email) {
        MoniteurResponse moniteur = rest.postForEntity("/api/moniteurs",
                new MoniteurCreationRequest("Garnier", "Paul", email,
                        "motdepasse-solide", null, null),
                MoniteurResponse.class).getBody();
        rest.exchange("/api/moniteurs/" + moniteur.id() + "/statut", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("statut", "APPROVED")), MoniteurResponse.class);
        return moniteur.id();
    }

    private UUID creerVoiture() {
        return rest.postForEntity("/api/voitures",
                new VoitureRequest("Clio école", "Renault", Transmission.MANUELLE,
                        true, null, null, null, null, false, null),
                VoitureResponse.class).getBody().id();
    }

    private HttpHeaders enTetesPour(String email, String motDePasse) {
        LoginResponse login = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, motDePasse), LoginResponse.class).getBody();
        HttpHeaders enTetes = new HttpHeaders();
        enTetes.setBearerAuth(login.token());
        return enTetes;
    }

    private UUID creerSeance(UUID reservationId, UUID moniteurId, UUID voitureId,
                             LocalDate date, LocalTime hDeb, LocalTime hFin) {
        return rest.postForEntity("/api/seances",
                new SeanceCreationRequest(reservationId, moniteurId, voitureId, date, hDeb, hFin, null),
                SeanceResponse.class).getBody().id();
    }

    @Test
    void planification_avec_detection_de_conflit_de_creneau() {
        UUID reservationId = creerReservation("nina.roux@example.fr");
        UUID moniteurId = creerMoniteurApprouve("paul.garnier@example.fr");
        UUID voitureId = creerVoiture();

        // Séance 9h-10h : OK
        ResponseEntity<SeanceResponse> premiere = rest.postForEntity("/api/seances",
                new SeanceCreationRequest(reservationId, moniteurId, voitureId,
                        LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 0), null),
                SeanceResponse.class);
        assertThat(premiere.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(premiere.getBody().statut()).isEqualTo(StatutSeance.SCHEDULED);
        assertThat(premiere.getBody().moniteurNomComplet()).isEqualTo("Paul Garnier");

        // Même moniteur, créneau chevauchant 9h30-10h30 : refusé
        ResponseEntity<String> conflit = rest.postForEntity("/api/seances",
                new SeanceCreationRequest(reservationId, moniteurId, null,
                        LocalDate.of(2026, 8, 10), LocalTime.of(9, 30), LocalTime.of(10, 30), null),
                String.class);
        assertThat(conflit.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(conflit.getBody()).contains("déjà une séance");

        // Créneau adjacent 10h-11h : les bornes ne se chevauchent pas → OK
        ResponseEntity<SeanceResponse> adjacente = rest.postForEntity("/api/seances",
                new SeanceCreationRequest(reservationId, moniteurId, null,
                        LocalDate.of(2026, 8, 10), LocalTime.of(10, 0), LocalTime.of(11, 0), null),
                SeanceResponse.class);
        assertThat(adjacente.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Clôture de la première séance, puis transition terminale interdite
        ResponseEntity<SeanceResponse> terminee = rest.exchange(
                "/api/seances/" + premiere.getBody().id() + "/statut", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("statut", "COMPLETED")), SeanceResponse.class);
        assertThat(terminee.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> interdite = rest.exchange(
                "/api/seances/" + premiere.getBody().id() + "/statut", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("statut", "CANCELLED")), String.class);
        assertThat(interdite.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void une_seance_hors_periode_de_reservation_est_refusee() {
        UUID reservationId = creerReservation("eleve.seance2@example.fr");

        ResponseEntity<String> reponse = rest.postForEntity("/api/seances",
                new SeanceCreationRequest(reservationId, null, null,
                        LocalDate.of(2027, 3, 1), LocalTime.of(9, 0), LocalTime.of(10, 0), null),
                String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).contains("pendant la réservation");
    }

    @Test
    void un_moniteur_ne_voit_que_ses_propres_seances() {
        UUID moniteurAId = creerMoniteurApprouve("moniteur.a@example.fr");
        UUID moniteurBId = creerMoniteurApprouve("moniteur.b@example.fr");
        UUID reservationId = creerReservation("eleve.scoping@example.fr");
        UUID seanceAId = creerSeance(reservationId, moniteurAId, null,
                LocalDate.of(2026, 8, 12), LocalTime.of(9, 0), LocalTime.of(10, 0));
        UUID seanceBId = creerSeance(reservationId, moniteurBId, null,
                LocalDate.of(2026, 8, 12), LocalTime.of(11, 0), LocalTime.of(12, 0));

        HttpHeaders enTetesA = enTetesPour("moniteur.a@example.fr", "motdepasse-solide");

        ResponseEntity<SeanceResponse[]> liste = restAnonyme.exchange(url("/api/seances"),
                HttpMethod.GET, new HttpEntity<>(enTetesA), SeanceResponse[].class);
        assertThat(liste.getBody()).extracting(SeanceResponse::id).containsExactly(seanceAId);

        ResponseEntity<SeanceResponse> saPropre = restAnonyme.exchange(
                url("/api/seances/" + seanceAId), HttpMethod.GET,
                new HttpEntity<>(enTetesA), SeanceResponse.class);
        assertThat(saPropre.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> celleDunAutre = restAnonyme.exchange(
                url("/api/seances/" + seanceBId), HttpMethod.GET,
                new HttpEntity<>(enTetesA), String.class);
        assertThat(celleDunAutre.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void un_moniteur_confirme_sa_seance_mais_pas_celle_dun_autre() {
        UUID moniteurAId = creerMoniteurApprouve("moniteur.c@example.fr");
        UUID moniteurBId = creerMoniteurApprouve("moniteur.d@example.fr");
        UUID reservationId = creerReservation("eleve.confirmation@example.fr");
        UUID seanceAId = creerSeance(reservationId, moniteurAId, null,
                LocalDate.of(2026, 8, 13), LocalTime.of(9, 0), LocalTime.of(10, 0));
        UUID seanceBId = creerSeance(reservationId, moniteurBId, null,
                LocalDate.of(2026, 8, 13), LocalTime.of(11, 0), LocalTime.of(12, 0));

        HttpHeaders enTetesA = enTetesPour("moniteur.c@example.fr", "motdepasse-solide");

        ResponseEntity<SeanceResponse> confirmation = restAnonyme.exchange(
                url("/api/seances/" + seanceAId + "/validation-moniteur"), HttpMethod.PATCH,
                new HttpEntity<>(enTetesA), SeanceResponse.class);
        assertThat(confirmation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmation.getBody().validatedMoniteur()).isTrue();

        ResponseEntity<String> confirmationDunAutre = restAnonyme.exchange(
                url("/api/seances/" + seanceBId + "/validation-moniteur"), HttpMethod.PATCH,
                new HttpEntity<>(enTetesA), String.class);
        assertThat(confirmationDunAutre.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void un_client_ne_voit_que_ses_propres_seances() {
        UUID reservationAId = creerReservation("eleve.a.scoping@example.fr");
        UUID reservationBId = creerReservation("eleve.b.scoping@example.fr");
        UUID seanceAId = creerSeance(reservationAId, null, null,
                LocalDate.of(2026, 8, 15), LocalTime.of(9, 0), LocalTime.of(10, 0));
        UUID seanceBId = creerSeance(reservationBId, null, null,
                LocalDate.of(2026, 8, 15), LocalTime.of(11, 0), LocalTime.of(12, 0));

        HttpHeaders enTetesA = enTetesPour("eleve.a.scoping@example.fr", "motdepasse-solide");

        ResponseEntity<SeanceResponse[]> liste = restAnonyme.exchange(url("/api/seances"),
                HttpMethod.GET, new HttpEntity<>(enTetesA), SeanceResponse[].class);
        assertThat(liste.getBody()).extracting(SeanceResponse::id).containsExactly(seanceAId);

        ResponseEntity<SeanceResponse> saPropre = restAnonyme.exchange(
                url("/api/seances/" + seanceAId), HttpMethod.GET,
                new HttpEntity<>(enTetesA), SeanceResponse.class);
        assertThat(saPropre.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> celleDunAutre = restAnonyme.exchange(
                url("/api/seances/" + seanceBId), HttpMethod.GET,
                new HttpEntity<>(enTetesA), String.class);
        assertThat(celleDunAutre.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void un_client_confirme_sa_seance_mais_pas_celle_dun_autre() {
        UUID reservationAId = creerReservation("eleve.a.confirmation@example.fr");
        UUID reservationBId = creerReservation("eleve.b.confirmation@example.fr");
        UUID seanceAId = creerSeance(reservationAId, null, null,
                LocalDate.of(2026, 8, 16), LocalTime.of(9, 0), LocalTime.of(10, 0));
        UUID seanceBId = creerSeance(reservationBId, null, null,
                LocalDate.of(2026, 8, 16), LocalTime.of(11, 0), LocalTime.of(12, 0));

        HttpHeaders enTetesA = enTetesPour("eleve.a.confirmation@example.fr", "motdepasse-solide");

        ResponseEntity<SeanceResponse> confirmation = restAnonyme.exchange(
                url("/api/seances/" + seanceAId + "/validation-client"), HttpMethod.PATCH,
                new HttpEntity<>(enTetesA), SeanceResponse.class);
        assertThat(confirmation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmation.getBody().validatedClient()).isTrue();

        ResponseEntity<String> confirmationDunAutre = restAnonyme.exchange(
                url("/api/seances/" + seanceBId + "/validation-client"), HttpMethod.PATCH,
                new HttpEntity<>(enTetesA), String.class);
        assertThat(confirmationDunAutre.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void un_directeur_ne_peut_pas_utiliser_lendpoint_de_confirmation_moniteur() {
        UUID moniteurId = creerMoniteurApprouve("moniteur.e@example.fr");
        UUID reservationId = creerReservation("eleve.role@example.fr");
        UUID seanceId = creerSeance(reservationId, moniteurId, null,
                LocalDate.of(2026, 8, 14), LocalTime.of(9, 0), LocalTime.of(10, 0));

        // rest est authentifié DIRECTEUR (voir AbstractIntegrationTest) — cet
        // endpoint est réservé à l'auto-confirmation par le moniteur lui-même.
        ResponseEntity<String> reponse = rest.exchange(
                "/api/seances/" + seanceId + "/validation-moniteur", HttpMethod.PATCH,
                new HttpEntity<>(null, new HttpHeaders()), String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
