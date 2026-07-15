package app.autoeecoleconnect.controllers.dto;

import java.util.UUID;

import app.autoeecoleconnect.models.Transmission;
import app.autoeecoleconnect.models.Voiture;

public record VoitureResponse(
        UUID id,
        String nom,
        String marque,
        Transmission transmission,
        boolean doubleCommande,
        String carburant,
        String couleur,
        Integer nbPortes,
        Integer nbPassagers,
        boolean airConditionne,
        String note,
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
