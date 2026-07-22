package app.autoeecoleconnect.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Créée par le control-plane, qui a déjà garanti l'unicité globale du slug. */
public record AutoEcoleCreationInterneRequest(
        @NotBlank @Size(max = 255) String nom,
        @NotBlank @Size(max = 100) String slug,
        String adresse) {
}
