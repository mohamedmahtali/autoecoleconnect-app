package app.autoeecoleconnect.controllers;

import java.util.Map;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.VoitureRequest;
import app.autoeecoleconnect.controllers.dto.VoitureResponse;
import app.autoeecoleconnect.models.Transmission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class VoitureControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void cycle_de_vie_complet_dune_voiture() {
        VoitureRequest creation = new VoitureRequest("208 école 1", "Peugeot",
                Transmission.MANUELLE, true, "Essence", "Blanche", 5, 4, true, null);
        ResponseEntity<VoitureResponse> cree =
                rest.postForEntity("/api/voitures", creation, VoitureResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID id = cree.getBody().id();
        assertThat(cree.getBody().doubleCommande()).isTrue();

        VoitureRequest maj = new VoitureRequest("208 école 1", "Peugeot",
                Transmission.MANUELLE, true, "Essence", "Rouge", 5, 4, true, "repeinte");
        ResponseEntity<VoitureResponse> modifie = rest.exchange("/api/voitures/" + id,
                HttpMethod.PUT, new HttpEntity<>(maj), VoitureResponse.class);
        assertThat(modifie.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(modifie.getBody().couleur()).isEqualTo("Rouge");

        ResponseEntity<Void> supprime = rest.exchange("/api/voitures/" + id,
                HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(supprime.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/voitures/" + id, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void creer_sans_transmission_repond_400() {
        Map<String, Object> invalide = Map.of("nom", "Clio", "marque", "Renault");
        ResponseEntity<String> reponse =
                rest.postForEntity("/api/voitures", invalide, String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).contains("transmission");
    }
}
