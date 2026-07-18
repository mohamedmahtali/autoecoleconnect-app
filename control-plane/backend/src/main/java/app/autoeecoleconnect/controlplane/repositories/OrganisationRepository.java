package app.autoeecoleconnect.controlplane.repositories;

import app.autoeecoleconnect.controlplane.models.Organisation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {

    boolean existsByEmailGerant(String emailGerant);

    Optional<Organisation> findByEmailGerant(String emailGerant);

    // Fenêtre de rappel J+25 : trial se terminant dans [maintenant, maintenant+N jours]
    List<Organisation> findByStatutAndReminderSentFalseAndTrialEndsAtBetween(
            String statut, LocalDateTime debut, LocalDateTime fin);

    // Suspension J+30 : trial déjà expiré
    List<Organisation> findByStatutAndTrialEndsAtBefore(String statut, LocalDateTime limite);
}
