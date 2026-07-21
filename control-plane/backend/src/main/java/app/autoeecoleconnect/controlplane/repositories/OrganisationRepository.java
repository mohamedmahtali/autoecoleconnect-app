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

    // Fenêtre de rappel J-5 (reminderJoursAvant) : trial se terminant dans
    // [maintenant, maintenant+N jours] — le commentaire précédent disait à
    // tort "J+25", qui est en fait le nouveau rappel précoce ci-dessous.
    List<Organisation> findByStatutAndReminderSentFalseAndTrialEndsAtBetween(
            String statut, LocalDateTime debut, LocalDateTime fin);

    // Rappel précoce J-25 (docs/16-backlog.md §16.3 item 16), même fenêtre
    // glissante que ci-dessus mais avec le nouveau flag dédié.
    List<Organisation> findByStatutAndReminderPrecoceSentFalseAndTrialEndsAtBetween(
            String statut, LocalDateTime debut, LocalDateTime fin);

    // Suspension J+30 : trial déjà expiré
    List<Organisation> findByStatutAndTrialEndsAtBefore(String statut, LocalDateTime limite);

    Optional<Organisation> findByStripeCustomerId(String stripeCustomerId);
}
