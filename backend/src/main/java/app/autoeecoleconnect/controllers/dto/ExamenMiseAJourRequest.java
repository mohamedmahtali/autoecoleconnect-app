package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDate;

import app.autoeecoleconnect.models.ResultatExamen;
import app.autoeecoleconnect.models.TypeExamen;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// Pas de clientId : on ne réassigne pas un examen à un autre élève. Sert
// surtout à renseigner le résultat et le nombre de fautes une fois l'examen
// passé (PLANIFIE -> REUSSI/ECHOUE/ABSENT).
public record ExamenMiseAJourRequest(
        @NotNull TypeExamen type,
        @NotNull LocalDate dateExamen,
        LocalDate dateConvocation,
        ResultatExamen resultat,
        @PositiveOrZero Integer nombreFautes,
        String centreExamen,
        String examinateur,
        String notes) {
}
