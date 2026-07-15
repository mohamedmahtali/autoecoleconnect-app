package app.autoeecoleconnect.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import app.autoeecoleconnect.controllers.dto.ForfaitRequest;
import app.autoeecoleconnect.controllers.dto.ForfaitResponse;
import app.autoeecoleconnect.controllers.dto.ReservationCreationRequest;
import app.autoeecoleconnect.controllers.dto.ReservationResponse;
import app.autoeecoleconnect.models.CarburantForfait;
import app.autoeecoleconnect.models.CategorieForfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.models.StatutReservation;
import app.autoeecoleconnect.models.UniteValidite;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationControllerIntegrationTest extends AbstractIntegrationTest {

    private UUID creerClient(String email) {
        ResponseEntity<ClientResponse> cree = rest.postForEntity("/api/clients",
                new ClientCreationRequest("Petit", "Léa", email, "motdepasse-solide",
                        null, null, null),
                ClientResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return cree.getBody().id();
    }

    private UUID creerForfait() {
        ResponseEntity<ForfaitResponse> cree = rest.postForEntity("/api/forfaits",
                new ForfaitRequest("Forfait résa 20h", 20, 6, UniteValidite.MOIS,
                        new BigDecimal("890.00"), null, CategorieForfait.CONDUITE, null,
                        Kilometrage.ILLIMITE, null, CarburantForfait.INCLUS),
                ForfaitResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return cree.getBody().id();
    }

    @Test
    void creer_puis_annuler_une_reservation() {
        UUID clientId = creerClient("lea.petit@example.fr");
        UUID forfaitId = creerForfait();

        // Création : dateFin et montant déduits du forfait (6 mois, 890 €)
        ResponseEntity<ReservationResponse> cree = rest.postForEntity("/api/reservations",
                new ReservationCreationRequest(clientId, forfaitId,
                        LocalDate.of(2026, 8, 1), null, null, null),
                ReservationResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ReservationResponse reservation = cree.getBody();
        assertThat(reservation.dateFin()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(reservation.montant()).isEqualByComparingTo("890.00");
        assertThat(reservation.statut()).isEqualTo(StatutReservation.PENDING);
        assertThat(reservation.clientNomComplet()).isEqualTo("Léa Petit");

        // Annulation
        ResponseEntity<ReservationResponse> annulee = rest.postForEntity(
                "/api/reservations/" + reservation.id() + "/annulation", null,
                ReservationResponse.class);
        assertThat(annulee.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(annulee.getBody().statut()).isEqualTo(StatutReservation.CANCELLED);

        // Une réservation annulée ne peut pas être annulée à nouveau
        ResponseEntity<String> refus = rest.postForEntity(
                "/api/reservations/" + reservation.id() + "/annulation", null, String.class);
        assertThat(refus.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void creer_avec_un_forfait_inconnu_repond_404() {
        UUID clientId = creerClient("autre.eleve@example.fr");

        ResponseEntity<String> reponse = rest.postForEntity("/api/reservations",
                new ReservationCreationRequest(clientId, UUID.randomUUID(),
                        LocalDate.of(2026, 8, 1), null, null, null),
                String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(reponse.getBody()).contains("Forfait");
    }
}
