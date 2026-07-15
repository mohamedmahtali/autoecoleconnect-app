package app.autoeecoleconnect.controllers.dto;

import java.math.BigDecimal;

import app.autoeecoleconnect.models.CarburantForfait;
import app.autoeecoleconnect.models.CategorieForfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.models.Transmission;
import app.autoeecoleconnect.models.UniteValidite;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ForfaitRequest(
        @NotBlank @Size(max = 255) String nom,
        @NotNull @Positive Integer nombreHeure,
        @NotNull @Positive Integer validite,
        @NotNull UniteValidite unite,
        @NotNull @Positive BigDecimal prix,
        String conditions,
        @NotNull CategorieForfait categorie,
        Transmission transmission,
        @NotNull Kilometrage kilometrage,
        @Positive Integer nbKilometre,
        @NotNull CarburantForfait carburant) {
}
