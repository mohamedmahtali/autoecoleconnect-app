package app.autoeecoleconnect.repositories;

import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Directeur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirecteurRepository extends JpaRepository<Directeur, UUID> {

    Optional<Directeur> findByEmailAndActiveTrue(String email);
}
