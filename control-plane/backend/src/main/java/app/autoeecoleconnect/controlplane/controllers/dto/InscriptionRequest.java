package app.autoeecoleconnect.controlplane.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InscriptionRequest(
        @NotBlank(message = "Le nom de l'auto-école est obligatoire")
        String nomAutoEcole,

        @NotBlank(message = "L'email du gérant est obligatoire")
        @Email(message = "Email invalide")
        String emailGerant,

        @NotBlank(message = "Le plan est obligatoire")
        @Pattern(regexp = "solo|pro|groupe|reseau", message = "Plan inconnu")
        String plan,

        // 72 : limite BCrypt (les octets au-delà seraient ignorés silencieusement)
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères")
        String motDePasse) {
}
