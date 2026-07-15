package app.autoeecoleconnect.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ReservationCreationRequest;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.models.Forfait;
import app.autoeecoleconnect.models.Reservation;
import app.autoeecoleconnect.models.StatutReservation;
import app.autoeecoleconnect.models.UniteValidite;
import app.autoeecoleconnect.repositories.ClientRepository;
import app.autoeecoleconnect.repositories.ForfaitRepository;
import app.autoeecoleconnect.repositories.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ForfaitRepository forfaitRepository;

    @InjectMocks
    private ReservationService reservationService;

    private final UUID clientId = UUID.randomUUID();
    private final UUID forfaitId = UUID.randomUUID();

    private Forfait forfait(UniteValidite unite, int validite, String prix) {
        Forfait forfait = new Forfait();
        forfait.setUnite(unite);
        forfait.setValidite(validite);
        forfait.setPrix(new BigDecimal(prix));
        return forfait;
    }

    private void preparerReferences(Forfait forfait) {
        when(clientRepository.findByIdAndActiveTrue(clientId))
                .thenReturn(Optional.of(new Client()));
        when(forfaitRepository.findByIdAndActiveTrue(forfaitId))
                .thenReturn(Optional.of(forfait));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void la_date_de_fin_est_calculee_depuis_la_validite_en_mois_du_forfait() {
        preparerReferences(forfait(UniteValidite.MOIS, 6, "890.00"));

        Reservation reservation = reservationService.creer(new ReservationCreationRequest(
                clientId, forfaitId, LocalDate.of(2026, 7, 15), null, null, null));

        assertThat(reservation.getDateFin()).isEqualTo(LocalDate.of(2027, 1, 15));
    }

    @Test
    void la_date_de_fin_est_calculee_depuis_la_validite_en_jours_du_forfait() {
        preparerReferences(forfait(UniteValidite.JOUR, 30, "300.00"));

        Reservation reservation = reservationService.creer(new ReservationCreationRequest(
                clientId, forfaitId, LocalDate.of(2026, 7, 15), null, null, null));

        assertThat(reservation.getDateFin()).isEqualTo(LocalDate.of(2026, 8, 14));
    }

    @Test
    void le_montant_par_defaut_est_le_prix_du_forfait() {
        preparerReferences(forfait(UniteValidite.MOIS, 6, "890.00"));

        Reservation reservation = reservationService.creer(new ReservationCreationRequest(
                clientId, forfaitId, LocalDate.of(2026, 7, 15), null, null, null));

        assertThat(reservation.getMontant()).isEqualByComparingTo("890.00");
        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.PENDING);
    }

    @Test
    void un_montant_explicite_remise_est_conserve() {
        preparerReferences(forfait(UniteValidite.MOIS, 6, "890.00"));

        Reservation reservation = reservationService.creer(new ReservationCreationRequest(
                clientId, forfaitId, LocalDate.of(2026, 7, 15),
                new BigDecimal("790.00"), null, null));

        assertThat(reservation.getMontant()).isEqualByComparingTo("790.00");
    }

    @Test
    void creer_avec_un_client_inconnu_echoue() {
        when(clientRepository.findByIdAndActiveTrue(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.creer(new ReservationCreationRequest(
                clientId, forfaitId, LocalDate.of(2026, 7, 15), null, null, null)))
                .isInstanceOf(RessourceIntrouvableException.class)
                .hasMessageContaining("Client");
    }

    @Test
    void annuler_une_reservation_terminee_est_interdit() {
        UUID id = UUID.randomUUID();
        Reservation reservation = new Reservation();
        reservation.setStatut(StatutReservation.COMPLETED);
        when(reservationRepository.findByIdAndActiveTrue(id))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.annuler(id))
                .isInstanceOf(ValidationMetierException.class)
                .hasMessageContaining("COMPLETED");
    }
}
