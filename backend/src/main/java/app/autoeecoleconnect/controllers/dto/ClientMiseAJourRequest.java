package app.autoeecoleconnect.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Le mot de passe ne se change pas ici : un endpoint dédié arrivera avec l'authentification.
public record ClientMiseAJourRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String telephone,
        String adresse,
        String notes) {
}
