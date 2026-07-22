package app.autoeecoleconnect.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    // client et forfait sont LAZY et open-in-view est désactivé : on les charge
    // dans la même requête, sinon LazyInitializationException au mapping DTO.
    @EntityGraph(attributePaths = {"client", "forfait"})
    List<Reservation> findByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    @EntityGraph(attributePaths = {"client", "forfait"})
    Optional<Reservation> findByIdAndActiveTrueAndAutoEcoleId(UUID id, UUID autoEcoleId);

    @EntityGraph(attributePaths = {"client", "forfait"})
    List<Reservation> findByActiveTrueAndAutoEcoleIdAndClientId(UUID autoEcoleId, UUID clientId);

    @EntityGraph(attributePaths = {"client", "forfait"})
    Optional<Reservation> findByIdAndActiveTrueAndAutoEcoleIdAndClientId(UUID id, UUID autoEcoleId, UUID clientId);

    // Premier agrégat SUM/COUNT de ce codebase (docs/16-backlog.md §16.3
    // item 15) — CA réellement encaissé, pas simplement facturé.
    @Query("SELECT COALESCE(SUM(r.montant), 0) FROM Reservation r "
            + "WHERE r.active = true AND r.autoEcoleId = :autoEcoleId "
            + "AND r.paiementStatut = app.autoeecoleconnect.models.PaiementStatut.PAID")
    BigDecimal sumMontantPaye(@Param("autoEcoleId") UUID autoEcoleId);

    /**
     * Somme <b>toutes agences confondues</b> : c'est ce que le control-plane
     * demande pour le tableau consolide du gerant, une organisation vivant
     * desormais dans une seule base (docs/18 §18.3 lot 7). Nommee
     * explicitement, comme les comptages de quota, pour qu'un appel hors de
     * ce cadre saute aux yeux en revue.
     */
    @Query("SELECT COALESCE(SUM(r.montant), 0) FROM Reservation r "
            + "WHERE r.active = true "
            + "AND r.paiementStatut = app.autoeecoleconnect.models.PaiementStatut.PAID")
    BigDecimal sumMontantPayeToutesEcoles();
}
