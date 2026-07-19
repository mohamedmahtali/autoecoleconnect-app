package app.autoeecoleconnect.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Quotas par plan d'abonnement (docs : grille tarifaire du 18/07/2026).
 * Le plan et le statut d'essai sont injectés par le chart Helm via
 * TENANT_PLAN / TENANT_TRIAL ; la grille elle-même vit dans application.yml
 * et reste surchargeable par env en cas d'exception commerciale.
 */
@ConfigurationProperties(prefix = "app.quotas")
public record QuotaProperties(
        String plan,
        boolean trial,
        Limites essai,
        Map<String, Limites> plans) {

    /** Une limite négative signifie « illimité ». */
    public record Limites(int eleves, int moniteurs, int vehicules) {

        public static final Limites ILLIMITEES = new Limites(-1, -1, -1);
    }
}
