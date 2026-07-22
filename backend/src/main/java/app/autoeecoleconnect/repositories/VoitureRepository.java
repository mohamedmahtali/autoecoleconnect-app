package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Voiture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VoitureRepository extends JpaRepository<Voiture, UUID> {

    List<Voiture> findByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    /** Comptage toutes agences confondues, reserve aux quotas (docs/17 §17.6). */
    @Query("SELECT COUNT(v) FROM Voiture v WHERE v.active = true")
    long compterActifsToutesEcoles();

    Optional<Voiture> findByIdAndActiveTrueAndAutoEcoleId(UUID id, UUID autoEcoleId);
}
