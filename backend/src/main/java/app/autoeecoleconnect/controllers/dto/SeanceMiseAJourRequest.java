package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

// Reprogrammation : la réservation d'origine ne change pas.
public record SeanceMiseAJourRequest(
        UUID moniteurId,
        UUID voitureId,
        @NotNull LocalDate dateSeance,
        @NotNull LocalTime hDeb,
        @NotNull LocalTime hFin,
        String notes) {
}
