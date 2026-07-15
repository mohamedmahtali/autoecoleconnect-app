package app.autoeecoleconnect.models;

import java.util.Map;
import java.util.Set;

/**
 * Workflow de modération d'un moniteur :
 * PENDING → APPROVED | REJECTED, puis APPROVED ↔ INACTIVE.
 */
public enum StatutMoniteur {
    PENDING,
    APPROVED,
    REJECTED,
    INACTIVE;

    private static final Map<StatutMoniteur, Set<StatutMoniteur>> TRANSITIONS = Map.of(
            PENDING, Set.of(APPROVED, REJECTED),
            APPROVED, Set.of(INACTIVE),
            INACTIVE, Set.of(APPROVED),
            REJECTED, Set.of());

    public boolean peutDevenir(StatutMoniteur cible) {
        return TRANSITIONS.get(this).contains(cible);
    }
}
