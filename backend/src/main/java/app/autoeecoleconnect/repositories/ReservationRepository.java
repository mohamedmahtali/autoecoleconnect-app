package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
