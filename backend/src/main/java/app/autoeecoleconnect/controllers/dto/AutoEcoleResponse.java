package app.autoeecoleconnect.controllers.dto;

import java.util.UUID;

import app.autoeecoleconnect.models.AutoEcole;

public record AutoEcoleResponse(UUID id, String nom, String slug, String adresse) {

    public static AutoEcoleResponse depuis(AutoEcole agence) {
        return new AutoEcoleResponse(
                agence.getId(), agence.getNom(), agence.getSlug(), agence.getAdresse());
    }
}
