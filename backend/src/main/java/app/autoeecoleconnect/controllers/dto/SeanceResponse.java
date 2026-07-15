package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import app.autoeecoleconnect.models.Seance;
import app.autoeecoleconnect.models.StatutSeance;

public record SeanceResponse(
        UUID id,
        UUID reservationId,
        String clientNomComplet,
        UUID moniteurId,
        String moniteurNomComplet,
        UUID voitureId,
        String voitureNom,
        LocalDate dateSeance,
        LocalTime hDeb,
        LocalTime hFin,
        StatutSeance statut,
        boolean validatedClient,
        boolean validatedMoniteur,
        boolean validatedAdmin,
        String notes,
        boolean active,
        LocalDateTime createdAt) {

    public static SeanceResponse depuis(Seance seance) {
        var client = seance.getReservation().getClient();
        var moniteur = seance.getMoniteur();
        var voiture = seance.getVoiture();
        return new SeanceResponse(
                seance.getId(),
                seance.getReservation().getId(),
                client.getPrenom() + " " + client.getNom(),
                moniteur != null ? moniteur.getId() : null,
                moniteur != null ? moniteur.getPrenom() + " " + moniteur.getNom() : null,
                voiture != null ? voiture.getId() : null,
                voiture != null ? voiture.getNom() : null,
                seance.getDateSeance(),
                seance.getHDeb(),
                seance.getHFin(),
                seance.getStatut(),
                seance.isValidatedClient(),
                seance.isValidatedMoniteur(),
                seance.isValidatedAdmin(),
                seance.getNotes(),
                seance.isActive(),
                seance.getCreatedAt());
    }
}
