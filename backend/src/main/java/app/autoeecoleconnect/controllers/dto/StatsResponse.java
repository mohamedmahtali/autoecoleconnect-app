package app.autoeecoleconnect.controllers.dto;

import java.math.BigDecimal;
import java.util.List;

// Version réaliste de docs/13-analytics.md — seuls les KPI calculables avec
// les données déjà en base (pas de résultats d'examen ni de disponibilités
// moniteur, qui n'existent nulle part encore). docs/16-backlog.md §16.3.
public record StatsResponse(
        BigDecimal caTotal,
        long elevesActifs,
        long seancesTerminees,
        long seancesNoShow,
        double tauxNoShow,
        List<InscriptionMensuelle> inscriptionsParMois) {

    public record InscriptionMensuelle(String mois, long nombre) {
    }
}
