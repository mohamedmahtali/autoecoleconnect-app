package app.autoeecoleconnect.repositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Seance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeanceRepository extends JpaRepository<Seance, UUID> {

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    List<Seance> findByActiveTrue();

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    Optional<Seance> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    List<Seance> findByActiveTrueAndMoniteurId(UUID moniteurId);

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    Optional<Seance> findByIdAndActiveTrueAndMoniteurId(UUID id, UUID moniteurId);

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    List<Seance> findByActiveTrueAndReservationClientId(UUID clientId);

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    Optional<Seance> findByIdAndActiveTrueAndReservationClientId(UUID id, UUID clientId);

    // Deux créneaux se chevauchent si chacun commence avant la fin de l'autre.
    // Seules les séances planifiées bloquent un créneau.
    @Query("""
            SELECT s FROM Seance s
            WHERE s.moniteur.id = :moniteurId
              AND s.dateSeance = :dateSeance
              AND s.active = true
              AND s.statut = app.autoeecoleconnect.models.StatutSeance.SCHEDULED
              AND s.hDeb < :hFin
              AND s.hFin > :hDeb
            """)
    List<Seance> seancesEnConflitPourMoniteur(@Param("moniteurId") UUID moniteurId,
                                              @Param("dateSeance") LocalDate dateSeance,
                                              @Param("hDeb") LocalTime hDeb,
                                              @Param("hFin") LocalTime hFin);

    @Query("""
            SELECT s FROM Seance s
            WHERE s.voiture.id = :voitureId
              AND s.dateSeance = :dateSeance
              AND s.active = true
              AND s.statut = app.autoeecoleconnect.models.StatutSeance.SCHEDULED
              AND s.hDeb < :hFin
              AND s.hFin > :hDeb
            """)
    List<Seance> seancesEnConflitPourVoiture(@Param("voitureId") UUID voitureId,
                                             @Param("dateSeance") LocalDate dateSeance,
                                             @Param("hDeb") LocalTime hDeb,
                                             @Param("hFin") LocalTime hFin);
}
