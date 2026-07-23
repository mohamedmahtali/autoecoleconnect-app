package app.autoeecoleconnect.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ExamenCreationRequest;
import app.autoeecoleconnect.controllers.dto.ExamenMiseAJourRequest;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.models.Examen;
import app.autoeecoleconnect.models.ResultatExamen;
import app.autoeecoleconnect.repositories.ClientRepository;
import app.autoeecoleconnect.repositories.ExamenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final ClientRepository clientRepository;
    private final ContexteAutoEcole contexteAutoEcole;

    public ExamenService(ExamenRepository examenRepository, ClientRepository clientRepository,
                         ContexteAutoEcole contexteAutoEcole) {
        this.examenRepository = examenRepository;
        this.clientRepository = clientRepository;
        this.contexteAutoEcole = contexteAutoEcole;
    }

    @Transactional(readOnly = true)
    public List<Examen> lister() {
        return examenRepository.findByActiveTrueAndAutoEcoleId(contexteAutoEcole.courante());
    }

    @Transactional(readOnly = true)
    public Examen trouver(UUID id) {
        return examenRepository.findByIdAndActiveTrueAndAutoEcoleId(id, contexteAutoEcole.courante())
                .orElseThrow(() -> new RessourceIntrouvableException("Examen", id));
    }

    @Transactional(readOnly = true)
    public List<Examen> listerPourClient(UUID clientId) {
        return examenRepository.findByActiveTrueAndAutoEcoleIdAndClientId(contexteAutoEcole.courante(), clientId);
    }

    public Examen creer(ExamenCreationRequest request) {
        Client client = clientRepository
                .findByIdAndActiveTrueAndAutoEcoleId(request.clientId(), contexteAutoEcole.courante())
                .orElseThrow(() -> new RessourceIntrouvableException("Client", request.clientId()));
        Examen examen = new Examen();
        examen.setClient(client);
        appliquer(examen, request.type(), request.dateExamen(), request.dateConvocation(),
                request.resultat(), request.nombreFautes(), request.centreExamen(),
                request.examinateur(), request.notes());
        examen.setAutoEcoleId(contexteAutoEcole.courante());
        return examenRepository.save(examen);
    }

    public Examen mettreAJour(UUID id, ExamenMiseAJourRequest request) {
        Examen examen = trouver(id);
        appliquer(examen, request.type(), request.dateExamen(), request.dateConvocation(),
                request.resultat(), request.nombreFautes(), request.centreExamen(),
                request.examinateur(), request.notes());
        return examenRepository.save(examen);
    }

    // Champs communs à la création et à la mise à jour (l'élève n'est pas
    // réassignable : on ne modifie pas à qui appartient un examen).
    private void appliquer(Examen examen, app.autoeecoleconnect.models.TypeExamen type,
                           java.time.LocalDate dateExamen, java.time.LocalDate dateConvocation,
                           ResultatExamen resultat, Integer nombreFautes, String centreExamen,
                           String examinateur, String notes) {
        examen.setType(type);
        examen.setDateExamen(dateExamen);
        examen.setDateConvocation(dateConvocation);
        examen.setResultat(resultat != null ? resultat : ResultatExamen.PLANIFIE);
        examen.setNombreFautes(nombreFautes);
        examen.setCentreExamen(centreExamen);
        examen.setExaminateur(examinateur);
        examen.setNotes(notes);
    }

    // Soft delete : cohérent avec les autres entités (historique conservé).
    public void supprimer(UUID id) {
        Examen examen = trouver(id);
        examen.setActive(false);
        examenRepository.save(examen);
    }
}
