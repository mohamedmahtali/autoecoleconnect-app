package app.autoeecoleconnect.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.VoitureRequest;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.models.Voiture;
import app.autoeecoleconnect.repositories.VoitureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VoitureService {

    private final VoitureRepository voitureRepository;
    private final QuotaService quotaService;

    public VoitureService(VoitureRepository voitureRepository, QuotaService quotaService) {
        this.voitureRepository = voitureRepository;
        this.quotaService = quotaService;
    }

    @Transactional(readOnly = true)
    public List<Voiture> lister() {
        return voitureRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Voiture trouver(UUID id) {
        return voitureRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Voiture", id));
    }

    public Voiture creer(VoitureRequest request) {
        quotaService.verifierPeutAjouterVehicule();
        Voiture voiture = new Voiture();
        appliquer(voiture, request);
        return voitureRepository.save(voiture);
    }

    public Voiture mettreAJour(UUID id, VoitureRequest request) {
        Voiture voiture = trouver(id);
        appliquer(voiture, request);
        return voitureRepository.save(voiture);
    }

    public void supprimer(UUID id) {
        Voiture voiture = trouver(id);
        voiture.setActive(false);
        voitureRepository.save(voiture);
    }

    private void appliquer(Voiture voiture, VoitureRequest request) {
        voiture.setNom(request.nom());
        voiture.setMarque(request.marque());
        voiture.setTransmission(request.transmission());
        voiture.setDoubleCommande(Boolean.TRUE.equals(request.doubleCommande()));
        voiture.setCarburant(request.carburant());
        voiture.setCouleur(request.couleur());
        voiture.setNbPortes(request.nbPortes());
        voiture.setNbPassagers(request.nbPassagers());
        voiture.setAirConditionne(Boolean.TRUE.equals(request.airConditionne()));
        voiture.setNote(request.note());
    }
}
