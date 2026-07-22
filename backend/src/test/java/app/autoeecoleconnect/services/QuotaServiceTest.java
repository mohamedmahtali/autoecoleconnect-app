package app.autoeecoleconnect.services;

import java.util.Map;

import app.autoeecoleconnect.config.QuotaProperties;
import app.autoeecoleconnect.exceptions.QuotaAtteintException;
import app.autoeecoleconnect.repositories.ClientRepository;
import app.autoeecoleconnect.repositories.MoniteurRepository;
import app.autoeecoleconnect.repositories.VoitureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    private static final QuotaProperties.Limites ESSAI = new QuotaProperties.Limites(15, 2, 2);
    private static final Map<String, QuotaProperties.Limites> PLANS = Map.of(
            "solo", new QuotaProperties.Limites(80, 3, 3),
            "groupe", new QuotaProperties.Limites(-1, -1, -1));

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MoniteurRepository moniteurRepository;

    @Mock
    private VoitureRepository voitureRepository;

    private QuotaService service(String plan, boolean trial) {
        return new QuotaService(new QuotaProperties(plan, trial, ESSAI, PLANS),
                clientRepository, moniteurRepository, voitureRepository);
    }

    @Test
    void essai_bloque_le_troisieme_moniteur() {
        when(moniteurRepository.countByActiveTrue()).thenReturn(2L);

        assertThatThrownBy(() -> service("solo", true).verifierPeutAjouterMoniteur())
                .isInstanceOf(QuotaAtteintException.class)
                .hasMessageContaining("période d'essai")
                .hasMessageContaining("2 moniteurs");
    }

    @Test
    void essai_laisse_passer_sous_la_limite() {
        when(moniteurRepository.countByActiveTrue()).thenReturn(1L);

        assertThatCode(() -> service("solo", true).verifierPeutAjouterMoniteur())
                .doesNotThrowAnyException();
    }

    @Test
    void plan_solo_bloque_le_81e_eleve_apres_abonnement() {
        when(clientRepository.compterActifsToutesEcoles()).thenReturn(80L);

        assertThatThrownBy(() -> service("solo", false).verifierPeutAjouterEleve())
                .isInstanceOf(QuotaAtteintException.class)
                .hasMessageContaining("plan solo")
                .hasMessageContaining("80 élèves");
    }

    @Test
    void abonnement_debloque_les_quotas_du_plan() {
        // 2 véhicules existants : plafond de l'essai (2) mais pas du plan solo (3)
        when(voitureRepository.countByActiveTrue()).thenReturn(2L);

        assertThatCode(() -> service("solo", false).verifierPeutAjouterVehicule())
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service("solo", true).verifierPeutAjouterVehicule())
                .isInstanceOf(QuotaAtteintException.class);
    }

    @Test
    void plan_groupe_est_illimite() {
        when(clientRepository.compterActifsToutesEcoles()).thenReturn(100_000L);

        assertThatCode(() -> service("groupe", false).verifierPeutAjouterEleve())
                .doesNotThrowAnyException();
    }

    @Test
    void plan_absent_de_la_grille_ne_bloque_jamais() {
        when(clientRepository.compterActifsToutesEcoles()).thenReturn(100_000L);

        assertThatCode(() -> service("reseau", false).verifierPeutAjouterEleve())
                .doesNotThrowAnyException();
    }
}
