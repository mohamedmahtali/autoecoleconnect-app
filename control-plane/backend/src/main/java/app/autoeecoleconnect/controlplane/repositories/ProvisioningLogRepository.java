package app.autoeecoleconnect.controlplane.repositories;

import app.autoeecoleconnect.controlplane.models.ProvisioningLog;
import app.autoeecoleconnect.controlplane.models.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvisioningLogRepository extends JpaRepository<ProvisioningLog, UUID> {

    Optional<ProvisioningLog> findFirstByTenantAndActionOrderByCreatedAtDesc(Tenant tenant, String action);
}
