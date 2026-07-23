package app.autoeecoleconnect.models;

// PLANIFIE : convoqué, pas encore passé. ABSENT : ne s'est pas présenté.
// Le taux de réussite se calcule sur les présentés (REUSSI + ECHOUE), les
// PLANIFIE et ABSENT en sont exclus (StatsService).
public enum ResultatExamen {
    PLANIFIE,
    REUSSI,
    ECHOUE,
    ABSENT
}
