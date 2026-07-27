package app.autoeecoleconnect.controllers.dto;

import java.math.BigDecimal;
import java.util.UUID;

import app.autoeecoleconnect.models.CarburantForfait;
import app.autoeecoleconnect.models.CategorieForfait;
import app.autoeecoleconnect.models.Forfait;
import app.autoeecoleconnect.models.Kilometrage;
import app.autoeecoleconnect.models.Transmission;
import app.autoeecoleconnect.models.UniteValidite;
import io.swagger.v3.oas.annotations.media.Schema;

public record ForfaitResponse(
        UUID id,
        String nom,
        Integer nombreHeure,
        Integer validite,
        UniteValidite unite,
        BigDecimal prix,
        @Schema(nullable = true) String conditions,
        CategorieForfait categorie,
        @Schema(nullable = true) Transmission transmission,
        Kilometrage kilometrage,
        @Schema(nullable = true) Integer nbKilometre,
        CarburantForfait carburant,
        boolean active) {

    public static ForfaitResponse depuis(Forfait forfait) {
        return new ForfaitResponse(
                forfait.getId(),
                forfait.getNom(),
                forfait.getNombreHeure(),
                forfait.getValidite(),
                forfait.getUnite(),
                forfait.getPrix(),
                forfait.getConditions(),
                forfait.getCategorie(),
                forfait.getTransmission(),
                forfait.getKilometrage(),
                forfait.getNbKilometre(),
                forfait.getCarburant(),
                forfait.isActive());
    }
}
