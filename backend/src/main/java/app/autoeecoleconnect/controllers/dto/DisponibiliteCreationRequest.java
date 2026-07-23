package app.autoeecoleconnect.controllers.dto;

import java.time.LocalTime;
import java.util.UUID;

import app.autoeecoleconnect.models.JourSemaine;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record DisponibiliteCreationRequest(
        @NotNull UUID moniteurId,
        @NotNull JourSemaine jour,
        @NotNull LocalTime heureDebut,
        @NotNull LocalTime heureFin) {

    // Validation croisée : un créneau doit avoir une durée positive. Reconnue
    // par bean validation sur une méthode isXxx() -> 400 via
    // GlobalExceptionHandler (MethodArgumentNotValidException).
    @AssertTrue(message = "L'heure de fin doit être postérieure à l'heure de début")
    public boolean isPlageValide() {
        return heureDebut != null && heureFin != null && heureFin.isAfter(heureDebut);
    }
}
