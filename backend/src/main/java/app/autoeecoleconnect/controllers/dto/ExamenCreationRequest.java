package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDate;
import java.util.UUID;

import app.autoeecoleconnect.models.ResultatExamen;
import app.autoeecoleconnect.models.TypeExamen;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// resultat null vaut PLANIFIE (examen convoqué, pas encore passé) — géré côté
// service. nombreFautes/centre/examinateur sont facultatifs (souvent inconnus
// tant que l'examen n'a pas eu lieu).
public record ExamenCreationRequest(
        @NotNull UUID clientId,
        @NotNull TypeExamen type,
        @NotNull LocalDate dateExamen,
        LocalDate dateConvocation,
        ResultatExamen resultat,
        @PositiveOrZero Integer nombreFautes,
        String centreExamen,
        String examinateur,
        String notes) {
}
