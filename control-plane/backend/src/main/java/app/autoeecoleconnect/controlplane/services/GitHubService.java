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
}
