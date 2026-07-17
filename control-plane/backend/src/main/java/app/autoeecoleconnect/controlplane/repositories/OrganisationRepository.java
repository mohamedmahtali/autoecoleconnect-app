package app.autoeecoleconnect.controlplane.repositories;

import app.autoeecoleconnect.controlplane.models.Organisation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {

    boolean existsByEmailGerant(String emailGerant);
}
