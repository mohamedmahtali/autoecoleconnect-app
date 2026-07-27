package app.autoeecoleconnect.controllers.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import app.autoeecoleconnect.models.PaiementStatut;
import app.autoeecoleconnect.models.PaiementType;
import app.autoeecoleconnect.models.Reservation;
import app.autoeecoleconnect.models.StatutReservation;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReservationResponse(
        UUID id,
        UUID clientId,
        String clientNomComplet,
        UUID forfaitId,
        String forfaitNom,
        LocalDate dateDebut,
        LocalDate dateFin,
        LocalDateTime dateReservation,
        BigDecimal montant,
        @Schema(nullable = true) PaiementType paiementType,
        PaiementStatut paiementStatut,
        StatutReservation statut,
        @Schema(nullable = true) String notes,
        boolean active) {

    public static ReservationResponse depuis(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getClient().getId(),
                reservation.getClient().getPrenom() + " " + reservation.getClient().getNom(),
                reservation.getForfait().getId(),
                reservation.getForfait().getNom(),
                reservation.getDateDebut(),
                reservation.getDateFin(),
                reservation.getDateReservation(),
                reservation.getMontant(),
                reservation.getPaiementType(),
                reservation.getPaiementStatut(),
                reservation.getStatut(),
                reservation.getNotes(),
                reservation.isActive());
    }
}
