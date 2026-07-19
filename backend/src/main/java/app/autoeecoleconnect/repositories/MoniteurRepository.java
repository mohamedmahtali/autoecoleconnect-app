package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Moniteur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoniteurRepository extends JpaRepository<Moniteur, UUID> {

    List<Moniteur> findByActiveTrue();

    long countByActiveTrue();

    Optional<Moniteur> findByIdAndActiveTrue(UUID id);

    Optional<Moniteur> findByEmailAndActiveTrue(String email);

    boolean existsByEmail(String email);
}
