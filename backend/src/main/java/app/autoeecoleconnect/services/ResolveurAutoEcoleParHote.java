package app.autoeecoleconnect.services;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import app.autoeecoleconnect.repositories.AutoEcoleRepository;
import org.springframework.stereotype.Component;

/**
 * Traduit l'en-tête {@code Host} en agence (docs/18 §18.3 lot 3) : chaque
 * agence a son propre sous-domaine, {@code <slug>.autoecoleconnect.fr}, et le
 * slug d'agence est exactement le premier segment de l'hôte.
 *
 * <p>Sert les requêtes <b>sans jeton</b> — le catalogue public des forfaits,
 * la page de connexion — où rien d'autre ne dit de quelle agence il s'agit.
 * Pour une requête authentifiée, c'est le jeton qui fait foi
 * ({@link ContexteAutoEcole}).
 */
@Component
public class ResolveurAutoEcoleParHote {

    /**
     * Cache des correspondances trouvées uniquement. Ne jamais mémoriser une
     * absence : une agence créée après le démarrage doit être résolue dès son
     * premier appel, sans redémarrage ni invalidation à orchestrer.
     */
    private final Map<String, UUID> parSlug = new ConcurrentHashMap<>();

    private final AutoEcoleRepository autoEcoleRepository;

    public ResolveurAutoEcoleParHote(AutoEcoleRepository autoEcoleRepository) {
        this.autoEcoleRepository = autoEcoleRepository;
    }

    public Optional<UUID> resoudre(String enteteHote) {
        String slug = slugDepuis(enteteHote);
        if (slug == null) {
            return Optional.empty();
        }
        UUID connue = parSlug.get(slug);
        if (connue != null) {
            return Optional.of(connue);
        }
        return autoEcoleRepository.findBySlugAndActiveTrue(slug)
                .map(ecole -> {
                    parSlug.put(slug, ecole.getId());
                    return ecole.getId();
                });
    }

    /**
     * {@code lyon.autoecoleconnect.fr:8443} → {@code lyon}. Renvoie
     * {@code null} pour un hôte sans sous-domaine ({@code localhost}, une IP
     * nue) : il n'y a alors pas d'agence à en déduire.
     */
    private String slugDepuis(String enteteHote) {
        if (enteteHote == null || enteteHote.isBlank()) {
            return null;
        }
        String hote = enteteHote.split(":", 2)[0].trim().toLowerCase();
        int premierPoint = hote.indexOf('.');
        if (premierPoint <= 0) {
            return null;
        }
        return hote.substring(0, premierPoint);
    }
}
