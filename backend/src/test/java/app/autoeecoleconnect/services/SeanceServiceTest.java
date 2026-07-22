package app.autoeecoleconnect.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.SeanceCreationRequest;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.Moniteur;
import app.autoeecoleconnect.models.Reservation;
import app.autoeecoleconnect.models.Seance;
import app.autoeecoleconnect.models.StatutMoniteur;
import app.autoeecoleconnect.models.StatutReservation;
import app.autoeecoleconnect.models.StatutSeance;
import app.autoeecoleconnect.repositories.MoniteurRepository;
import app.autoeecoleconnect.repositories.ReservationRepository;
import app.autoeecoleconnect.repositories.SeanceRepository;
import app.autoeecoleconnect.repositories.VoitureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeanceServiceTest {

    @Mock
    private SeanceRepository seanceRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MoniteurRepository moniteurRepository;

    @Mock
    private VoitureRepository voitureRepository;

    @Mock
    private ContexteAutoEcole contexteAutoEcole;

    @InjectMocks
    private SeanceService seanceService;

    private final UUID reservationId = UUID.randomUUID();
    private final UUID moniteurId = UUID.randomUUID();

    private void preparerReservation(StatutReservation statut) {
        Reservation reservation = new Reservation();
        reservation.setDateDebut(LocalDate.of(2026, 8, 1));
        reservation.setDateFin(LocalDate.of(2027, 2, 1));
        reservation.setStatut(statut);
        when(reservationRepository.findByIdAndActiveTrue(reservationId))
                .thenReturn(Optional.of(reservation));
    }

    private Moniteur preparerMoniteur(StatutMoniteur statut) {
        Moniteur moniteur = new Moniteur();
        moniteur.setStatut(statut);
        when(moniteurRepository.findByIdAndActiveTrue(moniteurId))
                .thenReturn(Optional.of(moniteur));
        return moniteur;
    }

    private SeanceCreationRequest requete(UUID moniteur, LocalDate date,
                                          LocalTime hDeb, LocalTime hFin) {
        return new SeanceCreationRequest(reservationId, moniteur, null, date, hDeb, hFin, null);
    }

    @Test
    void creer_une_seance_sans_moniteur_ni_voiture_est_valide() {
        preparerReservation(StatutReservation.PENDING);
        when(seanceRepository.save(any(Seance.class))).thenAnswer(inv -> inv.getArgument(0));

        Seance seance = seanceService.creer(requete(null, LocalDate.of(2026, 8, 10),
                LocalTime.of(9, 0), LocalTime.of(10, 0)));

        assertThat(seance.getStatut()).isEqualTo(StatutSeance.SCHEDULED);
    }

    @Test
    void lheure_de_fin_doit_etre_apres_lheure_de_debut() {
        preparerReservation(StatutReservation.PENDING);

        assertThatThrownBy(() -> seanceService.creer(requete(null,
                LocalDate.of(2026, 8, 10), LocalTime.of(10, 0), LocalTime.of(9, 0))))
                .isInstanceOf(ValidationMetierException.class)
                .hasMessageContaining("heure de fin");
    }

    @Test
    void la_seance_doit_avoir_lieu_pendant_la_reservation() {
        preparerReservation(StatutReservation.ACTIVE);

        assertThatThrownBy(() -> seanceService.creer(requete(null,
                LocalDate.of(2027, 3, 1), LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .isInstanceOf(ValidationMetierException.class)
                .hasMessageContaining("pendant la réservation");
    }

    @Test
    void une_reservation_annulee_ne_peut_pas_recevoir_de_seance() {
        preparerReservation(StatutReservation.CANCELLED);

        assertThatThrownBy(() -> seanceService.creer(requete(null,
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .isInstanceOf(ValidationMetierException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void le_moniteur_doit_etre_approuve() {
        preparerReservation(StatutReservation.PENDING);
        preparerMoniteur(StatutMoniteur.PENDING);

        assertThatThrownBy(() -> seanceService.creer(requete(moniteurId,
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .isInstanceOf(ValidationMetierException.class)
                .hasMessageContaining("approuvé");
    }

    @Test
    void un_moniteur_deja_occupe_sur_le_creneau_est_refuse() {
        preparerReservation(StatutReservation.PENDING);
        preparerMoniteur(StatutMoniteur.APPROVED);
        Seance conflit = mock(Seance.class);
        when(conflit.getId()).thenReturn(UUID.randomUUID());
        when(seanceRepository.seancesEnConflitPourMoniteur(moniteurId,
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .thenReturn(List.of(conflit));

        assertThatThrownBy(() -> seanceService.creer(requete(moniteurId,
                LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .isInstanceOf(ValidationMetierException.class)
                .hasMessageContaining("déjà une séance");
    }
}
