package app.autoeecoleconnect.services;

import java.util.UUID;

import app.autoeecoleconnect.repositories.AutoEcoleRepository;
import org.springframework.stereotype.Component;

/**
 * Source de vérité unique du périmètre : quelle auto-école la requête en
 * cours a le droit de voir. Tout le filtrage en dépend (docs/18 §18.3 lot 1).
 *
 * <p><b>Pourquoi un {@code ThreadLocal} et pas un {@code @Filter} Hibernate</b>
 * — le plan initial prévoyait un filtre Hibernate activé par session. La
 * vérification préalable (docs/18 §18.6) l'a écarté : {@code open-in-view}
 * est à {@code false}, donc l'{@code EntityManager} n'est pas lié à la
 * requête HTTP mais à la transaction, ce qui rend son activation fiable
 * délicate ; et un {@code @Filter} ne couvre de toute façon ni les requêtes
 * natives ({@code ClientRepository.inscriptionsParMois}) ni les accès par
 * identifiant. La garantie retenue est plus simple et plus solide : <b>aucune
 * méthode de repository ne permet de lire sans préciser l'école</b>. Un oubli
 * ne compile pas, au lieu de fuiter silencieusement.
 *
 * <p>Le périmètre est posé par la couche web (en-tête {@code Host} au lot 3,
 * claim du JWT au lot 2). Tant que ces lots ne sont pas faits, il reste vide
 * et l'école par défaut — la seule existante — s'applique.
 */
@Component
public class ContexteAutoEcole {

    private static final ThreadLocal<UUID> COURANTE = new ThreadLocal<>();

    private final AutoEcoleRepository autoEcoleRepository;

    /**
     * L'école par défaut ne change jamais au cours d'une exécution (elle est
     * créée par la migration v1.1). La mémoriser évite une requête par appel
     * de service, sans invalidation à prévoir.
     */
    private volatile UUID parDefaut;

    public ContexteAutoEcole(AutoEcoleRepository autoEcoleRepository) {
        this.autoEcoleRepository = autoEcoleRepository;
    }

    /**
     * L'école dont les données sont visibles. Jamais {@code null} : à défaut
     * de périmètre explicite, c'est l'école par défaut — donc un filtrage
     * effectif, pas un accès à tout.
     */
    public UUID courante() {
        UUID explicite = COURANTE.get();
        return explicite != null ? explicite : parDefaut();
    }

    /** Posé par la couche web pour la durée d'une requête. */
    public void definir(UUID autoEcoleId) {
        COURANTE.set(autoEcoleId);
    }

    /**
     * À appeler dans un {@code finally} : les threads sont recyclés d'une
     * requête à l'autre, un périmètre laissé en place fuiterait sur la
     * requête suivante.
     */
    public void effacer() {
        COURANTE.remove();
    }

    private UUID parDefaut() {
        UUID connue = parDefaut;
        if (connue == null) {
            connue = autoEcoleRepository.findFirstByActiveTrueOrderByCreatedAt()
                    .orElseThrow(() -> new IllegalStateException(
                            "Aucune auto-école en base — la migration v1.1 n'a pas été appliquée"))
                    .getId();
            parDefaut = connue;
        }
        return connue;
    }
}
