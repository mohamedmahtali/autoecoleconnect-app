package app.autoeecoleconnect.controlplane.repositories;

import app.autoeecoleconnect.controlplane.models.Tenant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsBySlug(String slug);

    List<Tenant> findByStatut(String statut);

    List<Tenant> findByOrganisationId(UUID organisationId);

    // Suppression J+60 : tenants suspendus depuis plus de N jours
    List<Tenant> findByStatutAndSuspendedAtBefore(String statut, LocalDateTime limite);
}
