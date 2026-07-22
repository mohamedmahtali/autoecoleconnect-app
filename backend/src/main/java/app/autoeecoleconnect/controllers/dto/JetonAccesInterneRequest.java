package app.autoeecoleconnect.controllers.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Demande d'un jeton d'accès pour le gérant, émise par le control-plane après
 * qu'il a lui-même authentifié la personne (docs/18 §18.3 lot 5).
 */
public record JetonAccesInterneRequest(
        @NotBlank @Email String email,
        @NotBlank String nomComplet,
        @NotNull UUID autoEcoleId) {
}
