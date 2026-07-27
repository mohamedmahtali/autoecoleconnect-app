package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import app.autoeecoleconnect.models.Examen;
import app.autoeecoleconnect.models.ResultatExamen;
import app.autoeecoleconnect.models.TypeExamen;
import io.swagger.v3.oas.annotations.media.Schema;

public record ExamenResponse(
        UUID id,
        UUID clientId,
        String clientNomComplet,
        TypeExamen type,
        LocalDate dateExamen,
        @Schema(nullable = true) LocalDate dateConvocation,
        ResultatExamen resultat,
        @Schema(nullable = true) Integer nombreFautes,
        @Schema(nullable = true) String centreExamen,
        @Schema(nullable = true) String examinateur,
        @Schema(nullable = true) String notes,
        boolean active,
        LocalDateTime createdAt) {

    public static ExamenResponse depuis(Examen examen) {
        return new ExamenResponse(
                examen.getId(),
                examen.getClient().getId(),
                examen.getClient().getPrenom() + " " + examen.getClient().getNom(),
                examen.getType(),
                examen.getDateExamen(),
                examen.getDateConvocation(),
                examen.getResultat(),
                examen.getNombreFautes(),
                examen.getCentreExamen(),
                examen.getExaminateur(),
                examen.getNotes(),
                examen.isActive(),
                examen.getCreatedAt());
    }
}
