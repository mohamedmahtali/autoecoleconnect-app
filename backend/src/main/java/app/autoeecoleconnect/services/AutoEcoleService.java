package app.autoeecoleconnect.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.AutoEcole;
import app.autoeecoleconnect.repositories.AutoEcoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agences de l'organisation (docs/17). Leur création passe par le
 * control-plane, jamais directement par le tenant : c'est lui qui garantit
 * l'unicité globale du slug et qui publie le sous-domaine correspondant.
 */
@Service
@Transactional
public class AutoEcoleService {

    private final AutoEcoleRepository autoEcoleRepository;

    public AutoEcoleService(AutoEcoleRepository autoEcoleRepository) {
        this.autoEcoleRepository = autoEcoleRepository;
    }

    @Transactional(readOnly = true)
    public List<AutoEcole> lister() {
        return autoEcoleRepository.findByActiveTrueOrderByNom();
    }

    @Transactional(readOnly = true)
    public AutoEcole trouver(UUID id) {
        return autoEcoleRepository.findById(id)
                .filter(AutoEcole::isActive)
                .orElseThrow(() -> new RessourceIntrouvableException("AutoEcole", id));
    }

    /**
     * Idempotent sur le slug : le control-plane peut réémettre l'appel après
     * un échec réseau sans créer de doublon, et sans que l'unicité repose sur
     * la seule contrainte de base (qui donnerait une 500 peu parlante).
     */
    public AutoEcole creer(String nom, String slug, String adresse) {
        var existante = autoEcoleRepository.findBySlugAndActiveTrue(slug);
        if (existante.isPresent()) {
            return existante.get();
        }
        if (nom == null || nom.isBlank() || slug == null || slug.isBlank()) {
            throw new ValidationMetierException("Le nom et le slug de l'agence sont obligatoires");
        }
        AutoEcole agence = new AutoEcole();
        agence.setNom(nom);
        agence.setSlug(slug);
        agence.setAdresse(adresse);
        return autoEcoleRepository.save(agence);
    }
}
