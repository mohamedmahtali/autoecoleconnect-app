package app.autoeecoleconnect.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    // client et forfait sont LAZY et open-in-view est désactivé : on les charge
    // dans la même requête, sinon LazyInitializationException au mapping DTO.
    @EntityGraph(attributePaths = {"client", "forfait"})
    List<Reservation> findByActiveTrue();

    @EntityGraph(attributePaths = {"client", "forfait"})
    Optional<Reservation> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"client", "forfait"})
    List<Reservation> findByActiveTrueAndClientId(UUID clientId);

    @EntityGraph(attributePaths = {"client", "forfait"})
    Optional<Reservation> findByIdAndActiveTrueAndClientId(UUID id, UUID clientId);

    // Premier agrégat SUM/COUNT de ce codebase (docs/16-backlog.md §16.3
    // item 15) — CA réellement encaissé, pas simplement facturé.
    @Query("SELECT COALESCE(SUM(r.montant), 0) FROM Reservation r "
            + "WHERE r.active = true AND r.paiementStatut = app.autoeecoleconnect.models.PaiementStatut.PAID")
    BigDecimal sumMontantPaye();
}
