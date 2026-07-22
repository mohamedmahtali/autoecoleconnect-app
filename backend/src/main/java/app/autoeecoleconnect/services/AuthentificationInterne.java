package app.autoeecoleconnect.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Vérifie l'en-tête {@code X-Internal-Api-Key} des appels venant du
 * control-plane, qui n'a aucun JWT tenant à présenter (docs/16-backlog.md
 * §16.3.A). Le secret est partagé, scellé cluster-wide dans les deux charts.
 *
 * <p>Extrait de {@code StatsController} au lot 4, quand un second appelant
 * interne est apparu (création d'agence) : la comparaison en temps constant
 * est le genre de détail qu'on ne veut pas voir dupliqué puis diverger.
 */
@Component
public class AuthentificationInterne {

    private final String cleAttendue;

    public AuthentificationInterne(@Value("${app.internal-stats-api-key}") String cleAttendue) {
        this.cleAttendue = cleAttendue;
    }

    /**
     * Comparaison en temps constant : cet en-tête voyage sur des routes
     * publiques, une comparaison naïve fuiterait la clé octet par octet.
     * Une clé non configurée (vide) refuse tout, plutôt que d'accepter tout.
     */
    public boolean estValide(String cleAppelant) {
        if (cleAttendue == null || cleAttendue.isBlank() || cleAppelant == null) {
            return false;
        }
        return MessageDigest.isEqual(
                cleAttendue.getBytes(StandardCharsets.UTF_8),
                cleAppelant.getBytes(StandardCharsets.UTF_8));
    }
}
