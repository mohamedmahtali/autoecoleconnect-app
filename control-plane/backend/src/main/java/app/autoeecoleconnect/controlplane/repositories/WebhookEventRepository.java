package app.autoeecoleconnect.controlplane.repositories;

import app.autoeecoleconnect.controlplane.models.WebhookEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    boolean existsByStripeEventId(String stripeEventId);
}
