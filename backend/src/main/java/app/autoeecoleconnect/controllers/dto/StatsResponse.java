package app.autoeecoleconnect.controllers.dto;

import java.math.BigDecimal;
import java.util.List;

// KPI de docs/13-analytics.md. Le taux de réussite examen est arrivé avec le
// suivi des examens (backlog #34) ; il reste le taux d'occupation moniteur
// (#35), qui suppose un référentiel de disponibilités encore inexistant.
// examensPresentes = REUSSI + ECHOUE (les présentés) : sert de dénominateur
// au taux et permet au front de masquer la carte quand il vaut 0.
public record StatsResponse(
        BigDecimal caTotal,
        long elevesActifs,
        long seancesTerminees,
        long seancesNoShow,
        double tauxNoShow,
        long examensPresentes,
        double tauxReussiteExamen,
        List<InscriptionMensuelle> inscriptionsParMois) {

    public record InscriptionMensuelle(String mois, long nombre) {
    }
}
