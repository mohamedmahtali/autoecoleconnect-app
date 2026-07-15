package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDate;

import app.autoeecoleconnect.models.StatutClient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String telephone,
        @Past LocalDate dateNaissance,
        StatutClient statut) {
}
