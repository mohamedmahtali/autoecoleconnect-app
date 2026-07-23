package app.autoeecoleconnect.controllers.dto;

import java.math.BigDecimal;
import java.util.List;

// KPI de docs/13-analytics.md. Depuis les items 34 et 35, tous les KPI de la
// doc sont couverts : taux de réussite examen (34) et taux d'occupation
// moniteur (35).
// examensPresentes = REUSSI + ECHOUE (les présentés), dénominateur du taux de
// réussite. heuresDispoHebdo = total des heures déclarées disponibles par
// semaine, dénominateur du taux d'occupation ; les deux permettent au front de
// masquer la carte correspondante quand le dénominateur vaut 0.
// tauxOccupation = heures de séances des 7 derniers jours / heuresDispoHebdo.
public record StatsResponse(
        BigDecimal caTotal,
        long elevesActifs,
        long seancesTerminees,
        long seancesNoShow,
        double tauxNoShow,
        long examensPresentes,
        double tauxReussiteExamen,
        double heuresDispoHebdo,
        double tauxOccupation,
        List<InscriptionMensuelle> inscriptionsParMois) {

    public record InscriptionMensuelle(String mois, long nombre) {
    }
}
