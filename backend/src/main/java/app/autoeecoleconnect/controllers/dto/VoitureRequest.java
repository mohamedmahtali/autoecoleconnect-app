package app.autoeecoleconnect.controllers.dto;

import app.autoeecoleconnect.models.Transmission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VoitureRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String marque,
        @NotNull Transmission transmission,
        Boolean doubleCommande,
        @Size(max = 50) String carburant,
        @Size(max = 50) String couleur,
        @Positive Integer nbPortes,
        @Positive Integer nbPassagers,
        Boolean airConditionne,
        String note) {
}
