package app.autoeecoleconnect.controllers;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.MoniteurCreationRequest;
import app.autoeecoleconnect.controllers.dto.VoitureRequest;
import app.autoeecoleconnect.models.Transmission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quotas à zéro pour rendre le test indépendant de l'état accumulé dans le
 * PostgreSQL partagé (pattern singleton container) : toute création doit
 * répondre 409 dès la première tentative.
 */
@TestPropertySource(properties = {
        "app.quotas.plan=solo",
        "app.quotas.trial=true",
        "app.quotas.essai.eleves=0",
        "app.quotas.essai.moniteurs=0",
        "app.quotas.essai.vehicules=0"
})
class QuotaControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void quota_essai_atteint_refuse_un_eleve_en_409() {
        ClientCreationRequest creation = new ClientCreationRequest(
                "Quota", "Eleve", "quota.eleve@example.fr",
                "motdepasse-solide", null, null, null);

        ResponseEntity<String> reponse = rest.postForEntity("/api/clients", creation, String.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody()).contains("période d'essai");
    }

    @Test
    void quota_essai_atteint_refuse_un_moniteur_en_409() {
        MoniteurCreationRequest creation = new MoniteurCreationRequest(
                "Quota", "Moniteur", "quota.moniteur@example.fr",
                "motdepasse-solide", null, null);

        ResponseEntity<String> reponse = rest.postForEntity("/api/moniteurs", creation, String.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody()).contains("période d'essai");
    }

    @Test
    void quota_essai_atteint_refuse_un_vehicule_en_409() {
        VoitureRequest creation = new VoitureRequest(
                "Clio quota", "Renault", Transmission.MANUELLE,
                true, null, null, null, null, null, null);

        ResponseEntity<String> reponse = rest.postForEntity("/api/voitures", creation, String.class);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody()).contains("période d'essai");
    }
}
