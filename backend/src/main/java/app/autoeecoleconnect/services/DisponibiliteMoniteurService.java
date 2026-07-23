package app.autoeecoleconnect.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.DisponibiliteCreationRequest;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.models.DisponibiliteMoniteur;
import app.autoeecoleconnect.models.Moniteur;
import app.autoeecoleconnect.repositories.DisponibiliteMoniteurRepository;
import app.autoeecoleconnect.repositories.MoniteurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisponibiliteMoniteurService {

    private final DisponibiliteMoniteurRepository disponibiliteRepository;
    private final MoniteurRepository moniteurRepository;
    private final ContexteAutoEcole contexteAutoEcole;

    public DisponibiliteMoniteurService(DisponibiliteMoniteurRepository disponibiliteRepository,
                                        MoniteurRepository moniteurRepository,
                                        ContexteAutoEcole contexteAutoEcole) {
        this.disponibiliteRepository = disponibiliteRepository;
        this.moniteurRepository = moniteurRepository;
        this.contexteAutoEcole = contexteAutoEcole;
    }

    @Transactional(readOnly = true)
    public List<DisponibiliteMoniteur> lister() {
        return disponibiliteRepository.findByActiveTrueAndAutoEcoleId(contexteAutoEcole.courante());
    }

    @Transactional(readOnly = true)
    public DisponibiliteMoniteur trouver(UUID id) {
        return disponibiliteRepository.findByIdAndActiveTrueAndAutoEcoleId(id, contexteAutoEcole.courante())
                .orElseThrow(() -> new RessourceIntrouvableException("Disponibilité", id));
    }

    public DisponibiliteMoniteur creer(DisponibiliteCreationRequest request) {
        // Le moniteur doit appartenir à l'agence courante — 404 sinon, on ne
        // crée pas un créneau pour un moniteur d'une autre agence.
        Moniteur moniteur = moniteurRepository
                .findByIdAndActiveTrueAndAutoEcoleId(request.moniteurId(), contexteAutoEcole.courante())
                .orElseThrow(() -> new RessourceIntrouvableException("Moniteur", request.moniteurId()));
        DisponibiliteMoniteur disponibilite = new DisponibiliteMoniteur();
        disponibilite.setMoniteur(moniteur);
        disponibilite.setJour(request.jour());
        disponibilite.setHeureDebut(request.heureDebut());
        disponibilite.setHeureFin(request.heureFin());
        disponibilite.setAutoEcoleId(contexteAutoEcole.courante());
        return disponibiliteRepository.save(disponibilite);
    }

    // Soft delete : cohérent avec le reste (le créneau disparaît des listes et
    // du calcul d'occupation, sans casser d'éventuelles références historiques).
    public void supprimer(UUID id) {
        DisponibiliteMoniteur disponibilite = trouver(id);
        disponibilite.setActive(false);
        disponibiliteRepository.save(disponibilite);
    }
}
