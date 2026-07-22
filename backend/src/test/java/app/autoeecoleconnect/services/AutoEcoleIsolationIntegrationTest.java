package app.autoeecoleconnect.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.AbstractIntegrationTest;
import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.models.AutoEcole;
import app.autoeecoleconnect.models.CarburantForfait;
import app.autoeecoleconnect.models.CategorieForfait;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.models.Forfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.models.Moniteur;
import app.autoeecoleconnect.models.Transmission;
import app.autoeecoleconnect.models.UniteValidite;
import app.autoeecoleconnect.models.Voiture;
import app.autoeecoleconnect.repositories.AutoEcoleRepository;
import app.autoeecoleconnect.repositories.ForfaitRepository;
import app.autoeecoleconnect.repositories.MoniteurRepository;
import app.autoeecoleconnect.repositories.VoitureRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test central de la refonte du grain de tenancy (docs/18 §18.3 lot 1) : deux
 * agences dans la même base ne doivent jamais voir les données l'une de
 * l'autre.
 *
 * <p>Testé au niveau service et non HTTP, délibérément : au lot 1 le périmètre
 * n'est pas encore posé par la couche web (en-tête {@code Host} au lot 3,
 * claim du JWT au lot 2). C'est le mécanisme de filtrage lui-même qui est
 * vérifié ici, indépendamment de la façon dont il sera alimenté.
 */
class AutoEcoleIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AutoEcoleRepository autoEcoleRepository;

    @Autowired
    private ContexteAutoEcole contexteAutoEcole;

    @Autowired
    private MoniteurService moniteurService;

    @Autowired
    private MoniteurRepository moniteurRepository;

    @Autowired
    private VoitureService voitureService;

    @Autowired
    private VoitureRepository voitureRepository;

    @Autowired
    private ForfaitService forfaitService;

    @Autowired
    private ForfaitRepository forfaitRepository;

    @AfterEach
    void nettoyerLeContexte() {
        contexteAutoEcole.effacer();
    }

    private AutoEcole creerEcole(String slug) {
        AutoEcole ecole = new AutoEcole();
        ecole.setNom(slug);
        ecole.setSlug(slug);
        return autoEcoleRepository.save(ecole);
    }

    private UUID creerClientDans(UUID autoEcoleId, String email) {
        contexteAutoEcole.definir(autoEcoleId);
        return clientService.creer(new ClientCreationRequest(
                "Nom", "Prenom", email, "motdepasse-solide", null, null, null)).getId();
    }

    @Test
    void une_agence_ne_voit_pas_les_eleves_dune_autre() {
        AutoEcole lyon = creerEcole("isolation-lyon-" + UUID.randomUUID());
        AutoEcole bron = creerEcole("isolation-bron-" + UUID.randomUUID());

        UUID eleveLyon = creerClientDans(lyon.getId(), "lyon-" + UUID.randomUUID() + "@example.fr");
        UUID eleveBron = creerClientDans(bron.getId(), "bron-" + UUID.randomUUID() + "@example.fr");

        contexteAutoEcole.definir(lyon.getId());
        List<UUID> vusDepuisLyon = clientService.lister().stream().map(Client::getId).toList();

        contexteAutoEcole.definir(bron.getId());
        List<UUID> vusDepuisBron = clientService.lister().stream().map(Client::getId).toList();

        assertThat(vusDepuisLyon).contains(eleveLyon).doesNotContain(eleveBron);
        assertThat(vusDepuisBron).contains(eleveBron).doesNotContain(eleveLyon);
    }

    @Test
    void acceder_a_un_eleve_dune_autre_agence_donne_introuvable_et_non_interdit() {
        AutoEcole lyon = creerEcole("acces-lyon-" + UUID.randomUUID());
        AutoEcole bron = creerEcole("acces-bron-" + UUID.randomUUID());
        UUID eleveBron = creerClientDans(bron.getId(), "bron-" + UUID.randomUUID() + "@example.fr");

        contexteAutoEcole.definir(lyon.getId());

        // Introuvable, pas « interdit » : l'existence même de la ressource ne
        // doit pas transparaître (docs/12 §12.5).
        assertThatThrownBy(() -> clientService.trouver(eleveBron))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void un_eleve_cree_est_rattache_a_lagence_courante() {
        AutoEcole lyon = creerEcole("rattachement-" + UUID.randomUUID());
        UUID eleve = creerClientDans(lyon.getId(), "rattache-" + UUID.randomUUID() + "@example.fr");

        contexteAutoEcole.definir(lyon.getId());
        assertThat(clientService.trouver(eleve).getAutoEcoleId()).isEqualTo(lyon.getId());
    }

    /**
     * Sans périmètre explicite, on retombe sur l'agence par défaut — donc un
     * filtrage effectif, jamais un accès à tout. C'est le comportement
     * transitoire du lot 1, décrit dans {@link ContexteAutoEcole}.
     */
    @Test
    void sans_perimetre_explicite_le_filtrage_reste_actif() {
        AutoEcole autre = creerEcole("defaut-" + UUID.randomUUID());
        UUID eleveAutre = creerClientDans(autre.getId(), "autre-" + UUID.randomUUID() + "@example.fr");

        contexteAutoEcole.effacer();

        assertThat(clientService.lister().stream().map(Client::getId).toList())
                .doesNotContain(eleveAutre);
    }

    // --- Les autres entités : créées directement en base avec leur agence,
    // --- puis lues via le service, qui est le seul à filtrer. ---

    @Test
    void une_agence_ne_voit_pas_les_moniteurs_dune_autre() {
        AutoEcole lyon = creerEcole("mon-lyon-" + UUID.randomUUID());
        AutoEcole bron = creerEcole("mon-bron-" + UUID.randomUUID());

        Moniteur chezBron = new Moniteur();
        chezBron.setNom("Nom");
        chezBron.setPrenom("Prenom");
        chezBron.setEmail("moniteur-" + UUID.randomUUID() + "@example.fr");
        chezBron.setPasswordHash("hash");
        chezBron.setAutoEcoleId(bron.getId());
        UUID idChezBron = moniteurRepository.save(chezBron).getId();

        contexteAutoEcole.definir(lyon.getId());

        assertThat(moniteurService.lister().stream().map(Moniteur::getId).toList())
                .doesNotContain(idChezBron);
        assertThatThrownBy(() -> moniteurService.trouver(idChezBron))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void une_agence_ne_voit_pas_les_vehicules_dune_autre() {
        AutoEcole lyon = creerEcole("voi-lyon-" + UUID.randomUUID());
        AutoEcole bron = creerEcole("voi-bron-" + UUID.randomUUID());

        Voiture chezBron = new Voiture();
        chezBron.setNom("Clio");
        chezBron.setMarque("Renault");
        chezBron.setTransmission(Transmission.MANUELLE);
        chezBron.setAutoEcoleId(bron.getId());
        UUID idChezBron = voitureRepository.save(chezBron).getId();

        contexteAutoEcole.definir(lyon.getId());

        assertThat(voitureService.lister().stream().map(Voiture::getId).toList())
                .doesNotContain(idChezBron);
        assertThatThrownBy(() -> voitureService.trouver(idChezBron))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void une_agence_ne_voit_pas_les_forfaits_dune_autre() {
        AutoEcole lyon = creerEcole("for-lyon-" + UUID.randomUUID());
        AutoEcole bron = creerEcole("for-bron-" + UUID.randomUUID());

        Forfait chezBron = new Forfait();
        chezBron.setNom("Forfait 20h");
        chezBron.setNombreHeure(20);
        chezBron.setValidite(6);
        chezBron.setUnite(UniteValidite.MOIS);
        chezBron.setPrix(new BigDecimal("1200.00"));
        chezBron.setCategorie(CategorieForfait.CONDUITE);
        chezBron.setTransmission(Transmission.MANUELLE);
        chezBron.setKilometrage(Kilometrage.ILLIMITE);
        chezBron.setCarburant(CarburantForfait.INCLUS);
        chezBron.setAutoEcoleId(bron.getId());
        UUID idChezBron = forfaitRepository.save(chezBron).getId();

        contexteAutoEcole.definir(lyon.getId());

        assertThat(forfaitService.lister().stream().map(Forfait::getId).toList())
                .doesNotContain(idChezBron);
        assertThatThrownBy(() -> forfaitService.trouver(idChezBron))
                .isInstanceOf(RessourceIntrouvableException.class);
    }
}
