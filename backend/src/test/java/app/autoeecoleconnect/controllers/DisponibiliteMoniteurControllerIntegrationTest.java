package app.autoeecoleconnect.controllers;

import java.time.LocalTime;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import app.autoeecoleconnect.controllers.dto.DisponibiliteCreationRequest;
import app.autoeecoleconnect.controllers.dto.DisponibiliteResponse;
import app.autoeecoleconnect.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controllers.dto.MoniteurCreationRequest;
import app.autoeecoleconnect.controllers.dto.MoniteurResponse;
import app.autoeecoleconnect.controllers.dto.StatsResponse;
import app.autoeecoleconnect.models.JourSemaine;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DisponibiliteMoniteurControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String MOT_DE_PASSE = "motdepasse-solide";

    private UUID creerMoniteur(String email) {
        ResponseEntity<MoniteurResponse> cree = rest.postForEntity("/api/moniteurs",
                new MoniteurCreationRequest("Roche", "Sami", email, MOT_DE_PASSE, null, null),
                MoniteurResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return cree.getBody().id();
    }

    private ResponseEntity<DisponibiliteResponse> creerCreneau(UUID moniteurId, JourSemaine jour,
                                                               LocalTime debut, LocalTime fin) {
        return rest.postForEntity("/api/disponibilites",
                new DisponibiliteCreationRequest(moniteurId, jour, debut, fin),
                DisponibiliteResponse.class);
    }

    @Test
    void creer_puis_lister_puis_supprimer_un_creneau() {
        UUID moniteurId = creerMoniteur("sami.roche.dispo@example.fr");

        ResponseEntity<DisponibiliteResponse> cree = creerCreneau(moniteurId, JourSemaine.LUNDI,
                LocalTime.of(9, 0), LocalTime.of(12, 0));
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(cree.getBody().moniteurNomComplet()).isEqualTo("Sami Roche");
        assertThat(cree.getBody().jour()).isEqualTo(JourSemaine.LUNDI);
        UUID creneauId = cree.getBody().id();

        ResponseEntity<DisponibiliteResponse[]> liste = rest.getForEntity("/api/disponibilites",
                DisponibiliteResponse[].class);
        assertThat(liste.getBody()).extracting(DisponibiliteResponse::id).contains(creneauId);

        rest.delete("/api/disponibilites/" + creneauId);
        ResponseEntity<DisponibiliteResponse[]> apres = rest.getForEntity("/api/disponibilites",
                DisponibiliteResponse[].class);
        assertThat(apres.getBody()).extracting(DisponibiliteResponse::id).doesNotContain(creneauId);
    }

    @Test
    void un_creneau_dont_la_fin_precede_le_debut_est_refuse() {
        UUID moniteurId = creerMoniteur("plage.invalide@example.fr");
        ResponseEntity<String> refus = rest.postForEntity("/api/disponibilites",
                new DisponibiliteCreationRequest(moniteurId, JourSemaine.MARDI,
                        LocalTime.of(18, 0), LocalTime.of(9, 0)),
                String.class);
        assertThat(refus.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void un_creneau_pour_un_moniteur_inconnu_repond_404() {
        ResponseEntity<String> refus = rest.postForEntity("/api/disponibilites",
                new DisponibiliteCreationRequest(UUID.randomUUID(), JourSemaine.LUNDI,
                        LocalTime.of(9, 0), LocalTime.of(12, 0)),
                String.class);
        assertThat(refus.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(refus.getBody()).contains("Moniteur");
    }

    @Test
    void un_eleve_ne_voit_pas_les_disponibilites() {
        String email = "eleve.sans.dispo@example.fr";
        rest.postForEntity("/api/clients",
                new ClientCreationRequest("Faure", "Lina", email, MOT_DE_PASSE, null, null, null),
                ClientResponse.class);
        LoginResponse login = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, MOT_DE_PASSE), LoginResponse.class).getBody();
        HttpHeaders entetes = new HttpHeaders();
        entetes.setBearerAuth(login.token());

        ResponseEntity<String> refus = restAnonyme.exchange(url("/api/disponibilites"),
                HttpMethod.GET, new HttpEntity<>(entetes), String.class);
        assertThat(refus.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void les_heures_declarees_alimentent_le_taux_d_occupation() {
        UUID moniteurId = creerMoniteur("kpi.occupation@example.fr");
        double dispoAvant = rest.getForEntity("/api/stats/resume", StatsResponse.class)
                .getBody().heuresDispoHebdo();

        // 3 h (9-12) + 4 h (14-18) = 7 h déclarées disponibles.
        creerCreneau(moniteurId, JourSemaine.LUNDI, LocalTime.of(9, 0), LocalTime.of(12, 0));
        creerCreneau(moniteurId, JourSemaine.LUNDI, LocalTime.of(14, 0), LocalTime.of(18, 0));

        StatsResponse apres = rest.getForEntity("/api/stats/resume", StatsResponse.class).getBody();
        assertThat(apres.heuresDispoHebdo()).isCloseTo(dispoAvant + 7.0, within(0.001));
        assertThat(apres.tauxOccupation()).isGreaterThanOrEqualTo(0.0);
    }
}
