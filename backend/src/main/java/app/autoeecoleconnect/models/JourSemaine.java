package app.autoeecoleconnect.models;

// Jour d'un créneau de disponibilité récurrent (backlog #35). Enum maison en
// français plutôt que java.time.DayOfWeek (qui stockerait MONDAY en base).
public enum JourSemaine {
    LUNDI,
    MARDI,
    MERCREDI,
    JEUDI,
    VENDREDI,
    SAMEDI,
    DIMANCHE
}
