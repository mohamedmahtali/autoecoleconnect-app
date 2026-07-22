package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import app.autoeecoleconnect.models.Directeur;

/**
 * Ne porte évidemment pas le hash du mot de passe. {@code autoEcoleId} est
 * exposé pour qu'un gérant multi-agences sache de quelle agence relève chaque
 * directeur.
 */
public record DirecteurResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        UUID autoEcoleId,
        LocalDateTime createdAt) {

    public static DirecteurResponse depuis(Directeur directeur) {
        return new DirecteurResponse(
                directeur.getId(),
                directeur.getNom(),
                directeur.getPrenom(),
                directeur.getEmail(),
                directeur.getAutoEcoleId(),
                directeur.getCreatedAt());
    }
}
