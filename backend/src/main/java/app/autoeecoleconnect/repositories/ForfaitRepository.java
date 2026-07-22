package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Forfait;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForfaitRepository extends JpaRepository<Forfait, UUID> {

    List<Forfait> findByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    Optional<Forfait> findByIdAndActiveTrueAndAutoEcoleId(UUID id, UUID autoEcoleId);
}
