package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.AutoEcole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoEcoleRepository extends JpaRepository<AutoEcole, UUID> {

    List<AutoEcole> findByActiveTrueOrderByNom();

    Optional<AutoEcole> findBySlugAndActiveTrue(String slug);

    Optional<AutoEcole> findFirstByActiveTrueOrderByCreatedAt();
}
