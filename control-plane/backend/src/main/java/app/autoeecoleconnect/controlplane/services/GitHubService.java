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
}
