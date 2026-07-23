package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import app.autoeecoleconnect.models.DisponibiliteMoniteur;
import app.autoeecoleconnect.models.JourSemaine;

public record DisponibiliteResponse(
        UUID id,
        UUID moniteurId,
        String moniteurNomComplet,
        JourSemaine jour,
        LocalTime heureDebut,
        LocalTime heureFin,
        boolean active,
        LocalDateTime createdAt) {

    public static DisponibiliteResponse depuis(DisponibiliteMoniteur d) {
        return new DisponibiliteResponse(
                d.getId(),
                d.getMoniteur().getId(),
                d.getMoniteur().getPrenom() + " " + d.getMoniteur().getNom(),
                d.getJour(),
                d.getHeureDebut(),
                d.getHeureFin(),
                d.isActive(),
                d.getCreatedAt());
    }
}
