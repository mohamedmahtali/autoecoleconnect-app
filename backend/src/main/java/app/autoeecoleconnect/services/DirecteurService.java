package app.autoeecoleconnect.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.DirecteurCreationRequest;
import app.autoeecoleconnect.exceptions.EmailDejaUtiliseException;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.Directeur;
import app.autoeecoleconnect.repositories.DirecteurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des comptes directeurs d'une agence (docs/16-backlog.md item 37).
 * Avant la refonte du grain de tenancy, un tenant n'avait qu'un directeur,
 * créé au démarrage et impossible à remplacer.
 */
@Service
@Transactional
public class DirecteurService {

    private final DirecteurRepository directeurRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContexteAutoEcole contexteAutoEcole;

    public DirecteurService(DirecteurRepository directeurRepository,
                            PasswordEncoder passwordEncoder,
                            ContexteAutoEcole contexteAutoEcole) {
        this.directeurRepository = directeurRepository;
        this.passwordEncoder = passwordEncoder;
        this.contexteAutoEcole = contexteAutoEcole;
    }

    @Transactional(readOnly = true)
    public List<Directeur> lister() {
        return directeurRepository.findByActiveTrueAndAutoEcoleId(contexteAutoEcole.courante());
    }

    @Transactional(readOnly = true)
    public Directeur trouver(UUID id) {
        return directeurRepository
                .findByIdAndActiveTrueAndAutoEcoleId(id, contexteAutoEcole.courante())
                .orElseThrow(() -> new RessourceIntrouvableException("Directeur", id));
    }

    public Directeur creer(DirecteurCreationRequest request) {
        // L'unicité de l'email est globale à la base (contrainte UNIQUE), donc
        // vérifiée sans filtre d'agence.
        if (directeurRepository.existsByEmail(request.email())) {
            throw new EmailDejaUtiliseException(request.email());
        }
        Directeur directeur = new Directeur();
        directeur.setNom(request.nom());
        directeur.setPrenom(request.prenom());
        directeur.setEmail(request.email());
        directeur.setPasswordHash(passwordEncoder.encode(request.motDePasse()));
        directeur.setAutoEcoleId(contexteAutoEcole.courante());
        return directeurRepository.save(directeur);
    }

    /**
     * Soft delete, avec un garde-fou : on refuse de désactiver le dernier
     * directeur d'une agence. Sans lui, un directeur pourrait supprimer son
     * propre compte et laisser l'agence sans personne pour y écrire — un
     * verrouillage dont seul un accès direct à la base permettrait de sortir.
     */
    public void supprimer(UUID id) {
        Directeur directeur = trouver(id);
        long restants = directeurRepository
                .countByActiveTrueAndAutoEcoleId(contexteAutoEcole.courante());
        if (restants <= 1) {
            throw new ValidationMetierException(
                    "Impossible de supprimer le dernier directeur de l'agence");
        }
        directeur.setActive(false);
        directeurRepository.save(directeur);
    }
}
