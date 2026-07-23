package app.autoeecoleconnect.controllers;

import java.time.LocalDate;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import app.autoeecoleconnect.controllers.dto.DonneesPersonnellesResponse;
import app.autoeecoleconnect.controllers.dto.ExamenCreationRequest;
import app.autoeecoleconnect.controllers.dto.ExamenMiseAJourRequest;
import app.autoeecoleconnect.controllers.dto.ExamenResponse;
import app.autoeecoleconnect.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controllers.dto.StatsResponse;
import app.autoeecoleconnect.models.ResultatExamen;
import app.autoeecoleconnect.models.TypeExamen;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ExamenControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String MOT_DE_PASSE = "motdepasse-solide";

    private UUID creerClient(String email) {
        ResponseEntity<ClientResponse> cree = rest.postForEntity("/api/clients",
                new ClientCreationRequest("Moreau", "Nadia", email, MOT_DE_PASSE, null, null, null),
                ClientResponse.class);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return cree.getBody().id();
    }

    private ResponseEntity<ExamenResponse> creerExamen(UUID clientId, TypeExamen type,
                                                       ResultatExamen resultat) {
        return rest.postForEntity("/api/examens",
                new ExamenCreationRequest(clientId, type, LocalDate.of(2026, 9, 15),
                        LocalDate.of(2026, 9, 1), resultat, null, "Centre de Lyon", "M. Petit", null),
                ExamenResponse.class);
    }

    @Test
    void creer_puis_lister_un_examen() {
        UUID clientId = creerClient("nadia.moreau.examen@example.fr");

        ResponseEntity<ExamenResponse> cree = creerExamen(clientId, TypeExamen.CONDUITE,
                ResultatExamen.PLANIFIE);
        assertThat(cree.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(cree.getBody().clientNomComplet()).isEqualTo("Nadia Moreau");
        assertThat(cree.getBody().type()).isEqualTo(TypeExamen.CONDUITE);
        assertThat(cree.getBody().resultat()).isEqualTo(ResultatExamen.PLANIFIE);
        assertThat(cree.getBody().centreExamen()).isEqualTo("Centre de Lyon");

        ResponseEntity<ExamenResponse[]> liste = rest.getForEntity("/api/examens", ExamenResponse[].class);
        assertThat(liste.getBody()).extracting(ExamenResponse::id).contains(cree.getBody().id());
    }

    @Test
    void mettre_a_jour_saisit_le_resultat_apres_l_examen() {
        UUID clientId = creerClient("resultat.apres@example.fr");
        UUID examenId = creerExamen(clientId, TypeExamen.CONDUITE, ResultatExamen.PLANIFIE).getBody().id();

        ResponseEntity<ExamenResponse> maj = rest.exchange("/api/examens/" + examenId,
                HttpMethod.PUT,
                new HttpEntity<>(new ExamenMiseAJourRequest(TypeExamen.CONDUITE,
                        LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 1),
                        ResultatExamen.ECHOUE, 6, "Centre de Lyon", "M. Petit", "2 fautes éliminatoires")),
                ExamenResponse.class);

        assertThat(maj.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(maj.getBody().resultat()).isEqualTo(ResultatExamen.ECHOUE);
        assertThat(maj.getBody().nombreFautes()).isEqualTo(6);
    }

    @Test
    void un_eleve_ne_voit_pas_les_examens() {
        String email = "eleve.sans.examens@example.fr";
        creerClient(email);
        LoginResponse login = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, MOT_DE_PASSE), LoginResponse.class).getBody();
        HttpHeaders entetes = new HttpHeaders();
        entetes.setBearerAuth(login.token());

        ResponseEntity<String> refus = restAnonyme.exchange(url("/api/examens"), HttpMethod.GET,
                new HttpEntity<>(entetes), String.class);
        assertThat(refus.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void seuls_les_presentes_comptent_dans_le_taux_de_reussite() {
        UUID clientId = creerClient("kpi.examen@example.fr");
        long presentesAvant = rest.getForEntity("/api/stats/resume", StatsResponse.class)
                .getBody().examensPresentes();

        // Sur 4 examens, seuls REUSSI et ECHOUE sont « présentés » (2) ;
        // ABSENT et PLANIFIE ne comptent pas au dénominateur.
        creerExamen(clientId, TypeExamen.CODE, ResultatExamen.REUSSI);
        creerExamen(clientId, TypeExamen.CODE, ResultatExamen.ECHOUE);
        creerExamen(clientId, TypeExamen.CONDUITE, ResultatExamen.ABSENT);
        creerExamen(clientId, TypeExamen.CONDUITE, ResultatExamen.PLANIFIE);

        StatsResponse apres = rest.getForEntity("/api/stats/resume", StatsResponse.class).getBody();
        assertThat(apres.examensPresentes()).isEqualTo(presentesAvant + 2);
        assertThat(apres.tauxReussiteExamen()).isBetween(0.0, 1.0);
    }

    @Test
    void l_export_rgpd_de_l_eleve_inclut_ses_examens() {
        String email = "export.avec.examens@example.fr";
        UUID clientId = creerClient(email);
        creerExamen(clientId, TypeExamen.CODE, ResultatExamen.REUSSI);

        LoginResponse login = restAnonyme.postForEntity(url("/api/auth/login"),
                new LoginRequest(email, MOT_DE_PASSE), LoginResponse.class).getBody();
        HttpHeaders entetes = new HttpHeaders();
        entetes.setBearerAuth(login.token());

        ResponseEntity<DonneesPersonnellesResponse> export = restAnonyme.exchange(
                url("/api/eleve/mes-donnees"), HttpMethod.GET, new HttpEntity<>(entetes),
                DonneesPersonnellesResponse.class);
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getBody().examens()).hasSize(1);
        assertThat(export.getBody().examens().get(0).resultat()).isEqualTo(ResultatExamen.REUSSI);
    }
}
