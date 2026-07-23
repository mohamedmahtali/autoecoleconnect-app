package app.autoeecoleconnect.services;

import app.autoeecoleconnect.controllers.dto.StatsResponse;
import app.autoeecoleconnect.models.ResultatExamen;
import app.autoeecoleconnect.models.StatutSeance;
import app.autoeecoleconnect.repositories.ClientRepository;
import app.autoeecoleconnect.repositories.DisponibiliteMoniteurRepository;
import app.autoeecoleconnect.repositories.ExamenRepository;
import app.autoeecoleconnect.repositories.ReservationRepository;
import app.autoeecoleconnect.repositories.SeanceRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final ReservationRepository reservationRepository;
    private final ClientRepository clientRepository;
    private final SeanceRepository seanceRepository;
    private final ExamenRepository examenRepository;
    private final DisponibiliteMoniteurRepository disponibiliteRepository;

    private final ContexteAutoEcole contexteAutoEcole;

    public StatsService(ReservationRepository reservationRepository,
                        ClientRepository clientRepository,
                        SeanceRepository seanceRepository,
                        ExamenRepository examenRepository,
                        DisponibiliteMoniteurRepository disponibiliteRepository,
                        ContexteAutoEcole contexteAutoEcole) {
        this.reservationRepository = reservationRepository;
        this.clientRepository = clientRepository;
        this.seanceRepository = seanceRepository;
        this.examenRepository = examenRepository;
        this.disponibiliteRepository = disponibiliteRepository;
        this.contexteAutoEcole = contexteAutoEcole;
    }

    /** Chiffres de l'agence courante — ce que voit un directeur. */
    public StatsResponse resume() {
        long terminees = seanceRepository.countByActiveTrueAndAutoEcoleIdAndStatut(contexteAutoEcole.courante(), StatutSeance.COMPLETED);
        long noShow = seanceRepository.countByActiveTrueAndAutoEcoleIdAndStatut(contexteAutoEcole.courante(), StatutSeance.NO_SHOW);
        long seancesCloturees = terminees + noShow;
        double tauxNoShow = seancesCloturees == 0 ? 0.0 : (double) noShow / seancesCloturees;

        // Taux de réussite sur les présentés (REUSSI + ECHOUE) — les PLANIFIE
        // et ABSENT sont hors dénominateur, comme le « taux de réussite au
        // permis » usuel (admis / présentés).
        long reussis = examenRepository.countByActiveTrueAndAutoEcoleIdAndResultat(contexteAutoEcole.courante(), ResultatExamen.REUSSI);
        long echoues = examenRepository.countByActiveTrueAndAutoEcoleIdAndResultat(contexteAutoEcole.courante(), ResultatExamen.ECHOUE);
        long presentes = reussis + echoues;
        double tauxReussite = presentes == 0 ? 0.0 : (double) reussis / presentes;

        // Taux d'occupation (#35) : heures de séances des 7 derniers jours
        // rapportées aux heures déclarées disponibles par semaine. Dénominateur
        // stable (créneaux récurrents), numérateur en fenêtre glissante — peut
        // dépasser 100 % en cas de sur-occupation (docs/13-analytics.md).
        double heuresDispoHebdo = disponibiliteRepository.sommeHeuresHebdo(contexteAutoEcole.courante());
        double heuresTravaillees = seanceRepository.sommeHeuresDepuis(
                contexteAutoEcole.courante(), LocalDate.now().minusDays(7));
        double tauxOccupation = heuresDispoHebdo == 0.0 ? 0.0 : heuresTravaillees / heuresDispoHebdo;

        List<StatsResponse.InscriptionMensuelle> inscriptions = clientRepository
                .inscriptionsParMois(contexteAutoEcole.courante())
                .stream()
                .map(ligne -> new StatsResponse.InscriptionMensuelle(
                        (String) ligne[0], ((Number) ligne[1]).longValue()))
                .toList();

        return new StatsResponse(
                reservationRepository.sumMontantPaye(contexteAutoEcole.courante()),
                clientRepository.countByActiveTrueAndAutoEcoleId(contexteAutoEcole.courante()),
                terminees,
                noShow,
                tauxNoShow,
                presentes,
                tauxReussite,
                heuresDispoHebdo,
                tauxOccupation,
                inscriptions);
    }

    /**
     * Chiffres de <b>toute l'organisation</b>, agences confondues — ce que le
     * control-plane demande pour le tableau consolidé du gérant (docs/18
     * §18.3 lot 7).
     *
     * <p>Depuis la refonte du grain de tenancy, une organisation vit dans une
     * seule base : le consolidé n'est donc plus une somme d'appels HTTP à
     * plusieurs tenants, mais une simple agrégation locale. Le control-plane
     * continue d'appeler le tenant — il est dans un autre namespace et une
     * autre base — mais une seule fois par organisation au lieu d'une fois
     * par agence.
     */
    public StatsResponse resumeOrganisation() {
        long terminees = seanceRepository.countByActiveTrueAndStatut(StatutSeance.COMPLETED);
        long noShow = seanceRepository.countByActiveTrueAndStatut(StatutSeance.NO_SHOW);
        long cloturees = terminees + noShow;
        double tauxNoShow = cloturees == 0 ? 0.0 : (double) noShow / cloturees;

        return new StatsResponse(
                reservationRepository.sumMontantPayeToutesEcoles(),
                clientRepository.compterActifsToutesEcoles(),
                terminees,
                noShow,
                tauxNoShow,
                // Le taux de reussite examen et le taux d'occupation alimentent
                // le dashboard d'une agence, pas la vue consolidee du gerant
                // (CA + eleves seulement) : on ne les agrege pas ici.
                0L,
                0.0,
                0.0,
                0.0,
                // Les inscriptions mensuelles servent le dashboard d'une agence,
                // pas le consolide : inutile de les agreger ici.
                List.of());
    }
}
