package app.autoeecoleconnect.controllers.dto;

import app.autoeecoleconnect.models.StatutSeance;
import jakarta.validation.constraints.NotNull;

public record ChangementStatutSeanceRequest(@NotNull StatutSeance statut) {
}
