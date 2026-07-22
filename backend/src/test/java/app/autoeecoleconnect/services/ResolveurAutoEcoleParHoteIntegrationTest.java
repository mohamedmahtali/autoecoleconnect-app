package app.autoeecoleconnect.services;

import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.models.AutoEcole;
import app.autoeecoleconnect.repositories.AutoEcoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Traduction de l'en-tête {@code Host} en agence (docs/18 §18.3 lot 3) —
 * ce qui permet à une requête sans jeton (catalogue public, connexion) de
 * savoir de quelle agence elle relève.
 */
class ResolveurAutoEcoleParHoteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ResolveurAutoEcoleParHote resolveur;

    @Autowired
    private AutoEcoleRepository autoEcoleRepository;

    private AutoEcole creerEcole(String slug) {
        AutoEcole ecole = new AutoEcole();
        ecole.setNom(slug);
        ecole.setSlug(slug);
        return autoEcoleRepository.save(ecole);
    }

    @Test
    void le_sous_domaine_designe_lagence() {
        String slug = "resolveur-" + UUID.randomUUID();
        AutoEcole ecole = creerEcole(slug);

        assertThat(resolveur.resoudre(slug + ".autoecoleconnect.fr"))
                .contains(ecole.getId());
    }

    @Test
    void le_port_et_la_casse_sont_ignores() {
        String slug = "port-" + UUID.randomUUID();
        AutoEcole ecole = creerEcole(slug);

        assertThat(resolveur.resoudre(slug.toUpperCase() + ".AutoEcoleConnect.fr:8443"))
                .contains(ecole.getId());
    }

    @Test
    void un_sous_domaine_inconnu_ne_designe_aucune_agence() {
        assertThat(resolveur.resoudre("agence-qui-nexiste-pas.autoecoleconnect.fr")).isEmpty();
    }

    @Test
    void un_hote_sans_sous_domaine_ne_designe_aucune_agence() {
        // localhost, une IP nue : rien n'en permet de déduire une agence, le
        // périmètre retombera sur l'agence par défaut.
        assertThat(resolveur.resoudre("localhost")).isEmpty();
        assertThat(resolveur.resoudre("localhost:8080")).isEmpty();
        assertThat(resolveur.resoudre(null)).isEmpty();
        assertThat(resolveur.resoudre("  ")).isEmpty();
    }

    /**
     * Le cache ne mémorise que les correspondances trouvées : une agence
     * créée après un premier appel infructueux doit être résolue sans
     * redémarrage.
     */
    @Test
    void une_agence_creee_apres_un_appel_infructueux_est_resolue() {
        String slug = "tardive-" + UUID.randomUUID();
        String hote = slug + ".autoecoleconnect.fr";

        assertThat(resolveur.resoudre(hote)).isEmpty();

        AutoEcole creee = creerEcole(slug);

        assertThat(resolveur.resoudre(hote)).contains(creee.getId());
    }
}
