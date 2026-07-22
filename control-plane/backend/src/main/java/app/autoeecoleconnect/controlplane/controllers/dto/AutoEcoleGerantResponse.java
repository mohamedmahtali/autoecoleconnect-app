package app.autoeecoleconnect.controlplane.controllers.dto;

import java.util.UUID;

import app.autoeecoleconnect.controlplane.models.AutoEcole;

public record AutoEcoleGerantResponse(UUID id, String nom, String slug, String url) {

    public static AutoEcoleGerantResponse depuis(AutoEcole agence, String domaine) {
        return new AutoEcoleGerantResponse(
                agence.getId(), agence.getNom(), agence.getSlug(),
                agence.getSlug() + "." + domaine);
    }
}
