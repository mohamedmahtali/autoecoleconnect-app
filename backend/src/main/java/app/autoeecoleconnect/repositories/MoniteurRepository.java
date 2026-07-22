package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Moniteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MoniteurRepository extends JpaRepository<Moniteur, UUID> {

    List<Moniteur> findByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    /**
     * Comptage <b>toutes agences confondues</b>, reserve aux quotas : le plan
     * est vendu a l'organisation (docs/17 §17.6 decision 2).
     */
    @Query("SELECT COUNT(m) FROM Moniteur m WHERE m.active = true")
    long compterActifsToutesEcoles();

    Optional<Moniteur> findByIdAndActiveTrueAndAutoEcoleId(UUID id, UUID autoEcoleId);

    Optional<Moniteur> findByEmailAndActiveTrue(String email);

    boolean existsByEmail(String email);
}
