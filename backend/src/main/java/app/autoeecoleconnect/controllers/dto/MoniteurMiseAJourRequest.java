package app.autoeecoleconnect.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Le statut se change via PATCH /api/moniteurs/{id}/statut, pas ici.
public record MoniteurMiseAJourRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String telephone,
        String notes) {
}
