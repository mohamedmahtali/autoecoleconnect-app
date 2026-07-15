package app.autoeecoleconnect.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ReservationCreationRequest;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.exceptions.ValidationMetierException;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.models.Forfait;
import app.autoeecoleconnect.models.Reservation;
import app.autoeecoleconnect.models.StatutReservation;
import app.autoeecoleconnect.repositories.ClientRepository;
import app.autoeecoleconnect.repositories.ForfaitRepository;
import app.autoeecoleconnect.repositories.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ClientRepository clientRepository;
    private final ForfaitRepository forfaitRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ClientRepository clientRepository,
                              ForfaitRepository forfaitRepository) {
        this.reservationRepository = reservationRepository;
        this.clientRepository = clientRepository;
        this.forfaitRepository = forfaitRepository;
    }

    @Transactional(readOnly = true)
    public List<Reservation> lister() {
        return reservationRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Reservation trouver(UUID id) {
        return reservationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Reservation", id));
    }

    public Reservation creer(ReservationCreationRequest request) {
        Client client = clientRepository.findByIdAndActiveTrue(request.clientId())
                .orElseThrow(() -> new RessourceIntrouvableException("Client", request.clientId()));
        Forfait forfait = forfaitRepository.findByIdAndActiveTrue(request.forfaitId())
                .orElseThrow(() -> new RessourceIntrouvableException("Forfait", request.forfaitId()));

        Reservation reservation = new Reservation();
        reservation.setClient(client);
        reservation.setForfait(forfait);
        reservation.setDateDebut(request.dateDebut());
        reservation.setDateFin(calculerDateFin(request.dateDebut(), forfait));
        // Sans montant explicite (remise, offre), on facture le prix du forfait
        reservation.setMontant(request.montant() != null ? request.montant() : forfait.getPrix());
        reservation.setPaiementType(request.paiementType());
        reservation.setNotes(request.notes());
        return reservationRepository.save(reservation);
    }

    public Reservation annuler(UUID id) {
        Reservation reservation = trouver(id);
        if (reservation.getStatut() != StatutReservation.PENDING
                && reservation.getStatut() != StatutReservation.ACTIVE) {
            throw new ValidationMetierException(
                    "Impossible d'annuler une réservation au statut %s"
                            .formatted(reservation.getStatut()));
        }
        reservation.setStatut(StatutReservation.CANCELLED);
        return reservationRepository.save(reservation);
    }

    public void supprimer(UUID id) {
        Reservation reservation = trouver(id);
        reservation.setActive(false);
        reservationRepository.save(reservation);
    }

    private LocalDate calculerDateFin(LocalDate dateDebut, Forfait forfait) {
        return switch (forfait.getUnite()) {
            case MOIS -> dateDebut.plusMonths(forfait.getValidite());
            case JOUR -> dateDebut.plusDays(forfait.getValidite());
        };
    }
}
