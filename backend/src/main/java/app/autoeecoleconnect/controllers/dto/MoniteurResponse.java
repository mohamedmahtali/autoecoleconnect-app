package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import app.autoeecoleconnect.models.Moniteur;
import app.autoeecoleconnect.models.StatutMoniteur;
import io.swagger.v3.oas.annotations.media.Schema;

// Ne jamais exposer passwordHash dans une réponse API.
public record MoniteurResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        @Schema(nullable = true) String telephone,
        StatutMoniteur statut,
        @Schema(nullable = true) String notes,
        boolean active,
        LocalDateTime createdAt) {

    public static MoniteurResponse depuis(Moniteur moniteur) {
        return new MoniteurResponse(
                moniteur.getId(),
                moniteur.getNom(),
                moniteur.getPrenom(),
                moniteur.getEmail(),
                moniteur.getTelephone(),
                moniteur.getStatut(),
                moniteur.getNotes(),
                moniteur.isActive(),
                moniteur.getCreatedAt());
    }
}
