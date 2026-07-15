package app.autoeecoleconnect.controllers.dto;

import app.autoeecoleconnect.models.StatutMoniteur;
import jakarta.validation.constraints.NotNull;

public record ChangementStatutMoniteurRequest(@NotNull StatutMoniteur statut) {
}
