package app.autoeecoleconnect.services;

import java.math.BigDecimal;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ForfaitRequest;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.CarburantForfait;
import app.autoeecoleconnect.models.CategorieForfait;
import app.autoeecoleconnect.models.Forfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.models.Transmission;
import app.autoeecoleconnect.models.UniteValidite;
import app.autoeecoleconnect.repositories.ForfaitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForfaitServiceTest {

    @Mock
    private ForfaitRepository forfaitRepository;

    @Mock
    private ContexteAutoEcole contexteAutoEcole;

    private static final UUID AUTO_ECOLE = UUID.randomUUID();

    @InjectMocks
    private ForfaitService forfaitService;

    @BeforeEach
    void perimetreParDefaut() {
        // lenient : toutes les methodes testees ne lisent pas le perimetre
        // (validations pures), et Mockito strict rejetterait un stub inutilise.
        lenient().when(contexteAutoEcole.courante()).thenReturn(AUTO_ECOLE);
    }

    private ForfaitRequest requete(Kilometrage kilometrage, Integer nbKilometre) {
        return new ForfaitRequest("Forfait 20h", 20, 6, UniteValidite.MOIS,
                new BigDecimal("890.00"), null, CategorieForfait.CONDUITE,
                Transmission.MANUELLE, kilometrage, nbKilometre, CarburantForfait.INCLUS);
    }

    @Test
    void creer_refuse_un_kilometrage_limite_sans_nombre_de_kilometres() {
        assertThatThrownBy(() -> forfaitService.creer(requete(Kilometrage.LIMITE, null)))
                .isInstanceOf(ValidationMetierException.class)
                .hasMessageContaining("nbKilometre");
        verify(forfaitRepository, never()).save(any());
    }

    @Test
    void creer_accepte_un_kilometrage_limite_avec_nombre_de_kilometres() {
        when(forfaitRepository.save(any(Forfait.class))).thenAnswer(inv -> inv.getArgument(0));

        Forfait forfait = forfaitService.creer(requete(Kilometrage.LIMITE, 500));

        assertThat(forfait.getNbKilometre()).isEqualTo(500);
    }

    @Test
    void creer_ignore_le_nombre_de_kilometres_pour_un_forfait_illimite() {
        when(forfaitRepository.save(any(Forfait.class))).thenAnswer(inv -> inv.getArgument(0));

        Forfait forfait = forfaitService.creer(requete(Kilometrage.ILLIMITE, 500));

        assertThat(forfait.getNbKilometre()).isNull();
    }
}
