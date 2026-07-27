package app.autoeecoleconnect.controllers.dto;

import java.util.UUID;

import app.autoeecoleconnect.models.Transmission;
import app.autoeecoleconnect.models.Voiture;
import io.swagger.v3.oas.annotations.media.Schema;

public record VoitureResponse(
        UUID id,
        String nom,
        String marque,
        Transmission transmission,
        boolean doubleCommande,
        @Schema(nullable = true) String carburant,
        @Schema(nullable = true) String couleur,
        @Schema(nullable = true) Integer nbPortes,
        @Schema(nullable = true) Integer nbPassagers,
        boolean airConditionne,
        @Schema(nullable = true) String note,
        boolean active) {

    public static VoitureResponse depuis(Voiture voiture) {
        return new VoitureResponse(
                voiture.getId(),
                voiture.getNom(),
                voiture.getMarque(),
                voiture.getTransmission(),
                voiture.isDoubleCommande(),
                voiture.getCarburant(),
                voiture.getCouleur(),
                voiture.getNbPortes(),
                voiture.getNbPassagers(),
                voiture.isAirConditionne(),
                voiture.getNote(),
                voiture.isActive());
    }
}
