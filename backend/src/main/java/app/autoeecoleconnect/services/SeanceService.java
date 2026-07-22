package app.autoeecoleconnect.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.SeanceCreationRequest;
import app.autoeecoleconnect.controllers.dto.SeanceMiseAJourRequest;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.Moniteur;
import app.autoeecoleconnect.models.Reservation;
import app.autoeecoleconnect.models.Seance;
import app.autoeecoleconnect.models.StatutMoniteur;
import app.autoeecoleconnect.models.StatutReservation;
import app.autoeecoleconnect.models.StatutSeance;
import app.autoeecoleconnect.models.Voiture;
import app.autoeecoleconnect.repositories.MoniteurRepository;
import app.autoeecoleconnect.repositories.ReservationRepository;
import app.autoeecoleconnect.repositories.SeanceRepository;
import app.autoeecoleconnect.repositories.VoitureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SeanceService {

    private final SeanceRepository seanceRepository;
    private final ReservationRepository reservationRepository;
    private final MoniteurRepository moniteurRepository;
    private final VoitureRepository voitureRepository;
    private final ContexteAutoEcole contexteAutoEcole;

    public SeanceService(SeanceRepository seanceRepository,
                         ReservationRepository reservationRepository,
                         MoniteurRepository moniteurRepository,
                         VoitureRepository voitureRepository,
                         ContexteAutoEcole contexteAutoEcole) {
        this.seanceRepository = seanceRepository;
        this.reservationRepository = reservationRepository;
        this.moniteurRepository = moniteurRepository;
        this.voitureRepository = voitureRepository;
        this.contexteAutoEcole = contexteAutoEcole;
    }

    @Transactional(readOnly = true)
    public List<Seance> lister() {
        return seanceRepository.findByActiveTrueAndAutoEcoleId(contexteAutoEcole.courante());
    }

    @Transactional(readOnly = true)
    public Seance trouver(UUID id) {
        return seanceRepository.findByIdAndActiveTrueAndAutoEcoleId(id, contexteAutoEcole.courante())
                .orElseThrow(() -> new RessourceIntrouvableException("Seance", id));
    }

    @Transactional(readOnly = true)
    public List<Seance> listerPourMoniteur(UUID moniteurId) {
        return seanceRepository.findByActiveTrueAndAutoEcoleIdAndMoniteurId(contexteAutoEcole.courante(), moniteurId);
    }

    @Transactional(readOnly = true)
    public Seance trouverPourMoniteur(UUID id, UUID moniteurId) {
        // 404 plutôt que 403 : ne révèle pas qu'une séance appartenant à un
        // autre moniteur existe.
        return seanceRepository.findByIdAndActiveTrueAndAutoEcoleIdAndMoniteurId(id, contexteAutoEcole.courante(), moniteurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Seance", id));
    }

    public Seance validerParMoniteur(UUID id, UUID moniteurId) {
        Seance seance = trouverPourMoniteur(id, moniteurId);
        if (seance.getStatut() != StatutSeance.SCHEDULED) {
            throw new ValidationMetierException(
                    "Seule une séance planifiée peut être confirmée (statut actuel : %s)"
                            .formatted(seance.getStatut()));
        }
        seance.setValidatedMoniteur(true);
        return seanceRepository.save(seance);
    }

    @Transactional(readOnly = true)
    public List<Seance> listerPourClient(UUID clientId) {
        return seanceRepository.findByActiveTrueAndAutoEcoleIdAndReservationClientId(contexteAutoEcole.courante(), clientId);
    }

    @Transactional(readOnly = true)
    public Seance trouverPourClient(UUID id, UUID clientId) {
        // 404 plutôt que 403 : ne révèle pas qu'une séance appartenant à un
        // autre élève existe (même raisonnement que pour le moniteur).
        return seanceRepository.findByIdAndActiveTrueAndAutoEcoleIdAndReservationClientId(id, contexteAutoEcole.courante(), clientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Seance", id));
    }

    public Seance validerParClient(UUID id, UUID clientId) {
        Seance seance = trouverPourClient(id, clientId);
        if (seance.getStatut() != StatutSeance.SCHEDULED) {
            throw new ValidationMetierException(
                    "Seule une séance planifiée peut être confirmée (statut actuel : %s)"
                            .formatted(seance.getStatut()));
        }
        seance.setValidatedClient(true);
        return seanceRepository.save(seance);
    }

    public Seance creer(SeanceCreationRequest request) {
        Reservation reservation = reservationRepository
                .findByIdAndActiveTrueAndAutoEcoleId(request.reservationId(), contexteAutoEcole.courante())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Reservation", request.reservationId()));
        if (reservation.getStatut() != StatutReservation.PENDING
                && reservation.getStatut() != StatutReservation.ACTIVE) {
            throw new ValidationMetierException(
                    "Impossible de planifier une séance sur une réservation au statut %s"
                            .formatted(reservation.getStatut()));
        }

        Seance seance = new Seance();
        seance.setReservation(reservation);
        appliquerCreneau(seance, request.moniteurId(), request.voitureId(),
                new Creneau(request.dateSeance(), request.hDeb(), request.hFin()),
                request.notes());
        seance.setAutoEcoleId(contexteAutoEcole.courante());
        return seanceRepository.save(seance);
    }

    public Seance mettreAJour(UUID id, SeanceMiseAJourRequest request) {
        Seance seance = trouver(id);
        if (seance.getStatut() != StatutSeance.SCHEDULED) {
            throw new ValidationMetierException(
                    "Seule une séance planifiée peut être reprogrammée (statut actuel : %s)"
                            .formatted(seance.getStatut()));
        }
        appliquerCreneau(seance, request.moniteurId(), request.voitureId(),
                new Creneau(request.dateSeance(), request.hDeb(), request.hFin()),
                request.notes());
        return seanceRepository.save(seance);
    }

    public Seance changerStatut(UUID id, StatutSeance cible) {
        Seance seance = trouver(id);
        if (!seance.getStatut().peutDevenir(cible)) {
            throw new ValidationMetierException(
                    "Transition de statut interdite : %s → %s"
                            .formatted(seance.getStatut(), cible));
        }
        seance.setStatut(cible);
        return seanceRepository.save(seance);
    }

    public void supprimer(UUID id) {
        Seance seance = trouver(id);
        seance.setActive(false);
        seanceRepository.save(seance);
    }

    private record Creneau(java.time.LocalDate date, java.time.LocalTime hDeb,
                           java.time.LocalTime hFin) {
    }

    private void appliquerCreneau(Seance seance, UUID moniteurId, UUID voitureId,
                                  Creneau creneau, String notes) {
        if (!creneau.hFin().isAfter(creneau.hDeb())) {
            throw new ValidationMetierException("L'heure de fin doit être après l'heure de début");
        }
        Reservation reservation = seance.getReservation();
        if (creneau.date().isBefore(reservation.getDateDebut())
                || creneau.date().isAfter(reservation.getDateFin())) {
            throw new ValidationMetierException(
                    "La séance doit avoir lieu pendant la réservation (%s → %s)"
                            .formatted(reservation.getDateDebut(), reservation.getDateFin()));
        }

        Moniteur moniteur = null;
        if (moniteurId != null) {
            moniteur = moniteurRepository.findByIdAndActiveTrueAndAutoEcoleId(moniteurId, contexteAutoEcole.courante())
                    .orElseThrow(() -> new RessourceIntrouvableException("Moniteur", moniteurId));
            if (moniteur.getStatut() != StatutMoniteur.APPROVED) {
                throw new ValidationMetierException(
                        "Le moniteur n'est pas approuvé (statut : %s)"
                                .formatted(moniteur.getStatut()));
            }
            verifierDisponibiliteMoniteur(seance, moniteurId, creneau);
        }

        Voiture voiture = null;
        if (voitureId != null) {
            voiture = voitureRepository.findByIdAndActiveTrueAndAutoEcoleId(voitureId, contexteAutoEcole.courante())
                    .orElseThrow(() -> new RessourceIntrouvableException("Voiture", voitureId));
            verifierDisponibiliteVoiture(seance, voitureId, creneau);
        }

        seance.setMoniteur(moniteur);
        seance.setVoiture(voiture);
        seance.setDateSeance(creneau.date());
        seance.setHDeb(creneau.hDeb());
        seance.setHFin(creneau.hFin());
        seance.setNotes(notes);
    }

    private void verifierDisponibiliteMoniteur(Seance seanceCourante, UUID moniteurId,
                                               Creneau creneau) {
        boolean conflit = seanceRepository
                .seancesEnConflitPourMoniteur(moniteurId, creneau.date(),
                        creneau.hDeb(), creneau.hFin())
                .stream()
                // en reprogrammation, la séance ne doit pas entrer en conflit avec elle-même
                .anyMatch(s -> !s.getId().equals(seanceCourante.getId()));
        if (conflit) {
            throw new ValidationMetierException(
                    "Le moniteur a déjà une séance sur ce créneau");
        }
    }

    private void verifierDisponibiliteVoiture(Seance seanceCourante, UUID voitureId,
                                              Creneau creneau) {
        boolean conflit = seanceRepository
                .seancesEnConflitPourVoiture(voitureId, creneau.date(),
                        creneau.hDeb(), creneau.hFin())
                .stream()
                .anyMatch(s -> !s.getId().equals(seanceCourante.getId()));
        if (conflit) {
            throw new ValidationMetierException(
                    "La voiture est déjà utilisée sur ce créneau");
        }
    }
}
