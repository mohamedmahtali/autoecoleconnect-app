package app.autoeecoleconnect.models;

import java.util.Map;
import java.util.Set;

/**
 * Cycle de vie d'une séance : SCHEDULED → COMPLETED | CANCELLED | NO_SHOW
 * (états terminaux).
 */
public enum StatutSeance {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    private static final Map<StatutSeance, Set<StatutSeance>> TRANSITIONS = Map.of(
            SCHEDULED, Set.of(COMPLETED, CANCELLED, NO_SHOW),
            COMPLETED, Set.of(),
            CANCELLED, Set.of(),
            NO_SHOW, Set.of());

    public boolean peutDevenir(StatutSeance cible) {
        return TRANSITIONS.get(this).contains(cible);
    }
}
