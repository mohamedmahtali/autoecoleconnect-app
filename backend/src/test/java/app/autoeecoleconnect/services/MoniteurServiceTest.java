package app.autoeecoleconnect.services;

import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.MoniteurCreationRequest;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.Moniteur;
import app.autoeecoleconnect.models.StatutMoniteur;
import app.autoeecoleconnect.repositories.MoniteurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoniteurServiceTest {

    @Mock
    private MoniteurRepository moniteurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private QuotaService quotaService;

    @InjectMocks
    private MoniteurService moniteurService;

    private Moniteur moniteurAvecStatut(UUID id, StatutMoniteur statut) {
        Moniteur moniteur = new Moniteur();
        moniteur.setStatut(statut);
        when(moniteurRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(moniteur));
        return moniteur;
    }

    @Test
    void un_moniteur_est_cree_avec_le_statut_pending() {
        when(moniteurRepository.existsByEmail("karim.b@example.fr")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        when(moniteurRepository.save(any(Moniteur.class))).thenAnswer(inv -> inv.getArgument(0));

        Moniteur moniteur = moniteurService.creer(new MoniteurCreationRequest(
                "Benali", "Karim", "karim.b@example.fr", "motdepasse-solide", null, null));

        assertThat(moniteur.getStatut()).isEqualTo(StatutMoniteur.PENDING);
    }

    @Test
    void approuver_un_moniteur_pending_est_autorise() {
        UUID id = UUID.randomUUID();
        moniteurAvecStatut(id, StatutMoniteur.PENDING);
        when(moniteurRepository.save(any(Moniteur.class))).thenAnswer(inv -> inv.getArgument(0));

        Moniteur approuve = moniteurService.changerStatut(id, StatutMoniteur.APPROVED);

        assertThat(approuve.getStatut()).isEqualTo(StatutMoniteur.APPROVED);
    }

    @Test
    void reactiver_un_moniteur_inactif_est_autorise() {
        UUID id = UUID.randomUUID();
        moniteurAvecStatut(id, StatutMoniteur.INACTIVE);
        when(moniteurRepository.save(any(Moniteur.class))).thenAnswer(inv -> inv.getArgument(0));

        Moniteur reactive = moniteurService.changerStatut(id, StatutMoniteur.APPROVED);

        assertThat(reactive.getStatut()).isEqualTo(StatutMoniteur.APPROVED);
    }

    @Test
    void approuver_un_moniteur_rejete_est_interdit() {
        UUID id = UUID.randomUUID();
        moniteurAvecStatut(id, StatutMoniteur.REJECTED);

        assertThatThrownBy(() -> moniteurService.changerStatut(id, StatutMoniteur.APPROVED))
                .isInstanceOf(ValidationMetierException.class)
                .hasMessageContaining("REJECTED")
                .hasMessageContaining("APPROVED");
    }

    @Test
    void desactiver_un_moniteur_pending_est_interdit() {
        UUID id = UUID.randomUUID();
        moniteurAvecStatut(id, StatutMoniteur.PENDING);

        assertThatThrownBy(() -> moniteurService.changerStatut(id, StatutMoniteur.INACTIVE))
                .isInstanceOf(ValidationMetierException.class);
    }
}
