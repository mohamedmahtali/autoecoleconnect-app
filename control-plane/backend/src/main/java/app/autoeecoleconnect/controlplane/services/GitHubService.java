package app.autoeecoleconnect.controlplane.services;

/**
 * Commit du fichier values.yaml d'un tenant dans le repo GitOps
 * (autoecoleconnect-infra) — voir docs/09-cycle-vie-tenant.md §9.2 étape 3.
 */
public interface GitHubService {

    /**
     * Crée (ou remplace) {@code tenants/<slug>/values.yaml} via l'API Contents
     * GitHub. Lève {@link app.autoeecoleconnect.controlplane.exceptions.ProvisioningException}
     * si le commit échoue.
     */
    void commitTenantValues(String slug, String valuesYamlContent);

    /**
     * Supprime {@code tenants/<slug>/values.yaml} (suppression J+60, docs/09
     * §9.7) — l'ApplicationSet ArgoCD prune alors toutes les ressources du
     * tenant, y compris sa base CNPG. Lève ProvisioningException en cas d'échec.
     */
    void deleteTenantValues(String slug);

    /**
     * Passe {@code trial: true} à {@code false} dans le values.yaml du tenant
     * à l'activation de l'abonnement Stripe : le backend tenant redémarre avec
     * TENANT_TRIAL=false et applique les quotas du plan au lieu de ceux de
     * l'essai. Idempotent (no-op si déjà à false ou fichier absent). Lève
     * ProvisioningException en cas d'échec.
     */
    void marquerTrialTermine(String slug);

    /**
     * Réécrit {@code tenant.autoEcoles} dans le values.yaml du tenant avec la
     * liste complète des slugs d'agences — la HTTPRoute expose alors un
     * sous-domaine par agence (docs/18 §18.3 lot 4).
     *
     * <p>La liste entière est passée plutôt qu'un ajout incrémental : l'appel
     * devient idempotent et ne peut pas produire de doublon si le
     * control-plane le rejoue après un échec réseau. Le YAML est reparsé puis
     * re-sérialisé, jamais modifié par substitution de texte, pour ne pas
     * dépendre de sa mise en forme.
     */
    void mettreAJourAutoEcoles(String slug, java.util.List<String> slugsAgences);
}
