package app.autoeecoleconnect.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ForfaitRequest;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.Forfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.repositories.ForfaitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ForfaitService {

    private final ForfaitRepository forfaitRepository;

    public ForfaitService(ForfaitRepository forfaitRepository) {
        this.forfaitRepository = forfaitRepository;
    }

    @Transactional(readOnly = true)
    public List<Forfait> lister() {
        return forfaitRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Forfait trouver(UUID id) {
        return forfaitRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Forfait", id));
    }

    public Forfait creer(ForfaitRequest request) {
        Forfait forfait = new Forfait();
        appliquer(forfait, request);
        return forfaitRepository.save(forfait);
    }

    public Forfait mettreAJour(UUID id, ForfaitRequest request) {
        Forfait forfait = trouver(id);
        appliquer(forfait, request);
        return forfaitRepository.save(forfait);
    }

    public void supprimer(UUID id) {
        Forfait forfait = trouver(id);
        forfait.setActive(false);
        forfaitRepository.save(forfait);
    }

    private void appliquer(Forfait forfait, ForfaitRequest request) {
        // Règle métier : un forfait à kilométrage limité doit préciser le nombre
        // de kilomètres ; un forfait illimité n'en a pas.
        if (request.kilometrage() == Kilometrage.LIMITE
                && (request.nbKilometre() == null || request.nbKilometre() <= 0)) {
            throw new ValidationMetierException(
                    "Un forfait à kilométrage limité doit préciser nbKilometre (> 0)");
        }
        forfait.setNom(request.nom());
        forfait.setNombreHeure(request.nombreHeure());
        forfait.setValidite(request.validite());
        forfait.setUnite(request.unite());
        forfait.setPrix(request.prix());
        forfait.setConditions(request.conditions());
        forfait.setCategorie(request.categorie());
        forfait.setTransmission(request.transmission());
        forfait.setKilometrage(request.kilometrage());
        forfait.setNbKilometre(
                request.kilometrage() == Kilometrage.LIMITE ? request.nbKilometre() : null);
        forfait.setCarburant(request.carburant());
    }
}
