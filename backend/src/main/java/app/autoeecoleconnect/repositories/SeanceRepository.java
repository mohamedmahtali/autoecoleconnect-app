package app.autoeecoleconnect.repositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Seance;
import app.autoeecoleconnect.models.StatutSeance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeanceRepository extends JpaRepository<Seance, UUID> {

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    List<Seance> findByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    Optional<Seance> findByIdAndActiveTrueAndAutoEcoleId(UUID id, UUID autoEcoleId);

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    List<Seance> findByActiveTrueAndAutoEcoleIdAndMoniteurId(UUID autoEcoleId, UUID moniteurId);

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    Optional<Seance> findByIdAndActiveTrueAndAutoEcoleIdAndMoniteurId(UUID id, UUID autoEcoleId, UUID moniteurId);

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    List<Seance> findByActiveTrueAndAutoEcoleIdAndReservationClientId(UUID autoEcoleId, UUID clientId);

    @EntityGraph(attributePaths = {"reservation", "reservation.client", "moniteur", "voiture"})
    Optional<Seance> findByIdAndActiveTrueAndAutoEcoleIdAndReservationClientId(UUID id, UUID autoEcoleId, UUID clientId);

    long countByActiveTrueAndAutoEcoleIdAndStatut(UUID autoEcoleId, StatutSeance statut);

    /** Toutes agences confondues — consolide du gerant (docs/18 §18.3 lot 7). */
    long countByActiveTrueAndStatut(StatutSeance statut);

    // Heures de séances effectivement occupées depuis une date (numérateur du
    // taux d'occupation, #35) : on exclut les annulées, on garde planifiées,
    // terminées et no-show (le créneau du moniteur était bien pris). Native :
    // arithmétique sur TIME, comme la somme des disponibilités.
    @Query(value = """
            SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (h_fin - h_deb)) / 3600), 0)
            FROM seances
            WHERE active = true AND auto_ecole_id = :autoEcoleId
              AND statut <> 'CANCELLED' AND date_seance >= :depuis
            """, nativeQuery = true)
    double sommeHeuresDepuis(@Param("autoEcoleId") UUID autoEcoleId, @Param("depuis") LocalDate depuis);

    // Deux créneaux se chevauchent si chacun commence avant la fin de l'autre.
    // Seules les séances planifiées bloquent un créneau.
    // ⚠️ Ces deux requêtes de conflit sont volontairement NON filtrées par
    // agence, à la différence de toutes les autres lectures : un moniteur ou
    // un véhicule partagé entre deux agences de la même organisation ne peut
    // pas être réservé deux fois à la même heure. Filtrer par agence ici
    // laisserait passer une double réservation — le seul cas où le
    // cloisonnement serait un défaut et non une garantie.
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
