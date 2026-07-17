package app.autoeecoleconnect.controlplane.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InscriptionRequest(
        @NotBlank(message = "Le nom de l'auto-école est obligatoire")
        String nomAutoEcole,

        @NotBlank(message = "L'email du gérant est obligatoire")
        @Email(message = "Email invalide")
        String emailGerant,

        @NotBlank(message = "Le plan est obligatoire")
        @Pattern(regexp = "solo|pro|groupe|reseau", message = "Plan inconnu")
        String plan) {
}
