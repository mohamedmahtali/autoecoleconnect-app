package app.autoeecoleconnect.services;

import app.autoeecoleconnect.config.QuotaProperties;
import app.autoeecoleconnect.exceptions.QuotaAtteintException;
import app.autoeecoleconnect.repositories.ClientRepository;
import app.autoeecoleconnect.repositories.MoniteurRepository;
import app.autoeecoleconnect.repositories.VoitureRepository;
import org.springframework.stereotype.Service;

/**
 * Applique les quotas du plan (ou de la période d'essai) à la création
 * d'élèves, de moniteurs et de véhicules. Seules les ressources actives
 * comptent : un soft delete libère la place. La souscription d'un abonnement
 * fait passer TENANT_TRIAL à false côté GitOps, ce qui débloque les quotas
 * du plan sans autre intervention.
 */
@Service
public class QuotaService {

    private final QuotaProperties properties;
    private final ClientRepository clientRepository;
    private final MoniteurRepository moniteurRepository;
    private final VoitureRepository voitureRepository;

    public QuotaService(QuotaProperties properties,
                        ClientRepository clientRepository,
                        MoniteurRepository moniteurRepository,
                        VoitureRepository voitureRepository) {
        this.properties = properties;
        this.clientRepository = clientRepository;
        this.moniteurRepository = moniteurRepository;
        this.voitureRepository = voitureRepository;
    }

    // ⚠️ Comptages volontairement NON filtrés par agence : depuis la refonte
    // du grain de tenancy, le plan est vendu à l'organisation et les quotas
    // s'appliquent à l'ensemble de ses écoles — « Pro = 250 élèves toutes
    // écoles confondues » (docs/17 §17.6 décision 2).
    public void verifierPeutAjouterEleve() {
        verifier(clientRepository.compterActifsToutesEcoles(), limites().eleves(), "élèves actifs");
    }

    public void verifierPeutAjouterMoniteur() {
        verifier(moniteurRepository.compterActifsToutesEcoles(), limites().moniteurs(), "moniteurs");
    }

    public void verifierPeutAjouterVehicule() {
        verifier(voitureRepository.compterActifsToutesEcoles(), limites().vehicules(), "véhicules");
    }

    private QuotaProperties.Limites limites() {
        if (properties.trial()) {
            return properties.essai();
        }
        // Plan inconnu (reseau, valeur future…) : pas de restriction plutôt
        // que de bloquer un client payant sur un trou de configuration.
        return properties.plans().getOrDefault(properties.plan(),
                QuotaProperties.Limites.ILLIMITEES);
    }

    private void verifier(long actuel, int limite, String ressource) {
        if (limite < 0 || actuel < limite) {
            return;
        }
        if (properties.trial()) {
            throw new QuotaAtteintException(
                    "Limite de la période d'essai atteinte : %d %s maximum. Abonnez-vous depuis votre espace gérant pour débloquer les quotas de votre plan."
                            .formatted(limite, ressource));
        }
        throw new QuotaAtteintException(
                "Limite du plan %s atteinte : %d %s maximum. Passez au plan supérieur depuis votre espace gérant."
                        .formatted(properties.plan(), limite, ressource));
    }
}
