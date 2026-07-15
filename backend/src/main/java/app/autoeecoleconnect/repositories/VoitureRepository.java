package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Voiture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoitureRepository extends JpaRepository<Voiture, UUID> {

    List<Voiture> findByActiveTrue();

    Optional<Voiture> findByIdAndActiveTrue(UUID id);
}
