package app.autoeecoleconnect.controllers;

import java.math.BigDecimal;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ForfaitRequest;
import app.autoeecoleconnect.controllers.dto.ForfaitResponse;
import app.autoeecoleconnect.models.CarburantForfait;
import app.autoeecoleconnect.models.CategorieForfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.models.Transmission;
import app.autoeecoleconnect.models.UniteValidite;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ForfaitControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void cycle_de_vie_complet_dun_forfait() {
        ForfaitRequest creation = new ForfaitRequest("Forfait 20h", 20, 6,
                UniteValidite.MOIS, new BigDecimal("890.00"), "Hors examens",
                CategorieForfait.CONDUITE, Transmission.MANUELLE,
                Kilometrage.ILLIMITE, null, CarburantForfait.INCLUS);
        ResponseEntity<ForfaitResponse> cree =
                rest.postForEntity("/api/forfaits", creation, ForfaitResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID id = cree.getBody().id();
        assertThat(cree.getBody().prix()).isEqualByComparingTo("890.00");

        ForfaitRequest maj = new ForfaitRequest("Forfait 20h", 20, 6,
                UniteValidite.MOIS, new BigDecimal("920.00"), "Hors examens",
                CategorieForfait.CONDUITE, Transmission.MANUELLE,
                Kilometrage.ILLIMITE, null, CarburantForfait.INCLUS);
        ResponseEntity<ForfaitResponse> modifie = rest.exchange("/api/forfaits/" + id,
                HttpMethod.PUT, new HttpEntity<>(maj), ForfaitResponse.class);
        assertThat(modifie.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(modifie.getBody().prix()).isEqualByComparingTo("920.00");

        ResponseEntity<Void> supprime = rest.exchange("/api/forfaits/" + id,
                HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(supprime.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/forfaits/" + id, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void creer_un_forfait_limite_sans_kilometres_repond_400() {
        ForfaitRequest invalide = new ForfaitRequest("Forfait limité", 10, 3,
                UniteValidite.MOIS, new BigDecimal("450.00"), null,
                CategorieForfait.CONDUITE, null,
                Kilometrage.LIMITE, null, CarburantForfait.NON_INCLUS);
        ResponseEntity<String> reponse =
                rest.postForEntity("/api/forfaits", invalide, String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).contains("nbKilometre");
    }
}
