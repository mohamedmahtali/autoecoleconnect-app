package app.autoeecoleconnect.controlplane.repositories;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controlplane.models.AutoEcole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoEcoleRepository extends JpaRepository<AutoEcole, UUID> {

    List<AutoEcole> findByTenantIdOrderByNom(UUID tenantId);

    boolean existsBySlug(String slug);
}
