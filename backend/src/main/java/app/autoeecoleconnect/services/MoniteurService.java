package app.autoeecoleconnect.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.MoniteurCreationRequest;
import app.autoeecoleconnect.controllers.dto.MoniteurMiseAJourRequest;
import app.autoeecoleconnect.exceptions.EmailDejaUtiliseException;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.Moniteur;
import app.autoeecoleconnect.models.StatutMoniteur;
import app.autoeecoleconnect.repositories.MoniteurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MoniteurService {

    private final MoniteurRepository moniteurRepository;
    private final PasswordEncoder passwordEncoder;

    public MoniteurService(MoniteurRepository moniteurRepository, PasswordEncoder passwordEncoder) {
        this.moniteurRepository = moniteurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Moniteur> lister() {
        return moniteurRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Moniteur trouver(UUID id) {
        return moniteurRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Moniteur", id));
    }

    public Moniteur creer(MoniteurCreationRequest request) {
        if (moniteurRepository.existsByEmail(request.email())) {
            throw new EmailDejaUtiliseException(request.email());
        }
        Moniteur moniteur = new Moniteur();
        moniteur.setNom(request.nom());
        moniteur.setPrenom(request.prenom());
        moniteur.setEmail(request.email());
        moniteur.setPasswordHash(passwordEncoder.encode(request.motDePasse()));
        moniteur.setTelephone(request.telephone());
        moniteur.setNotes(request.notes());
        return moniteurRepository.save(moniteur);
    }

    public Moniteur mettreAJour(UUID id, MoniteurMiseAJourRequest request) {
        Moniteur moniteur = trouver(id);
        if (!moniteur.getEmail().equals(request.email())
                && moniteurRepository.existsByEmail(request.email())) {
            throw new EmailDejaUtiliseException(request.email());
        }
        moniteur.setNom(request.nom());
        moniteur.setPrenom(request.prenom());
        moniteur.setEmail(request.email());
        moniteur.setTelephone(request.telephone());
        moniteur.setNotes(request.notes());
        return moniteurRepository.save(moniteur);
    }

    public Moniteur changerStatut(UUID id, StatutMoniteur cible) {
        Moniteur moniteur = trouver(id);
        if (!moniteur.getStatut().peutDevenir(cible)) {
            throw new ValidationMetierException(
                    "Transition de statut interdite : %s → %s"
                            .formatted(moniteur.getStatut(), cible));
        }
        moniteur.setStatut(cible);
        return moniteurRepository.save(moniteur);
    }

    public void supprimer(UUID id) {
        Moniteur moniteur = trouver(id);
        moniteur.setActive(false);
        moniteurRepository.save(moniteur);
    }
}
