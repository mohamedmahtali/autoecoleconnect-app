package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SeanceCreationRequest(
        @NotNull UUID reservationId,
        UUID moniteurId,
        UUID voitureId,
        @NotNull LocalDate dateSeance,
        @NotNull LocalTime hDeb,
        @NotNull LocalTime hFin,
        String notes) {
}
