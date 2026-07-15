package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByActiveTrue();

    Optional<Client> findByIdAndActiveTrue(UUID id);

    boolean existsByEmail(String email);
}
