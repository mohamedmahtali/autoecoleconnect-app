package app.autoeecoleconnect.controlplane.controllers.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Le slug n'est pas fourni : il est dérivé du nom et rendu unique globalement. */
public record AutoEcoleCreationRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(max = 255) String nom,
        String adresse) {
}
