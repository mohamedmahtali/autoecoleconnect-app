package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.config.ProvisioningProperties;
import app.autoeecoleconnect.controlplane.exceptions.ProvisioningException;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Commit via l'API Contents GitHub (repos/{owner}/{repo}/contents/{path}).
 * Pattern GET-sha-puis-PUT : idempotent, gère aussi bien la création que la
 * mise à jour du fichier.
 *
 * <p>Si {@code app.provisioning.github-pat} est vide (dev local / CI, aucun
 * PAT fourni), le commit est simulé (log uniquement) plutôt que d'échouer —
 * permet de faire tourner toute la chaîne de provisioning sans dépendre d'un
 * vrai repo GitHub.</p>
 */
@Service
public class GitHubServiceImpl implements GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubServiceImpl.class);

    private final RestClient restClient;
    private final ProvisioningProperties properties;

    public GitHubServiceImpl(ProvisioningProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + properties.githubPat())
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    @Override
    public void commitTenantValues(String slug, String valuesYamlContent) {
        if (properties.githubPat() == null || properties.githubPat().isBlank()) {
            log.info("[GITHUB_PAT absent] Commit simulé pour tenants/{}/values.yaml :\n{}",
                    slug, valuesYamlContent);
            return;
        }

        String path = "tenants/" + slug + "/values.yaml";
        String contentsUrl = "/repos/%s/%s/contents/%s".formatted(
                properties.githubOwner(), properties.githubRepo(), path);

        String sha = trouverShaExistant(contentsUrl);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "feat: provisioning tenant " + slug);
        body.put("content", Base64.getEncoder().encodeToString(
                valuesYamlContent.getBytes(StandardCharsets.UTF_8)));
        body.put("branch", "main");
        if (sha != null) {
            body.put("sha", sha);
        }

        try {
            restClient.put()
                    .uri(contentsUrl)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ProvisioningException(
                    "Échec du commit GitHub pour " + slug + " : HTTP " + e.getStatusCode(), e);
        }
    }

    @Override
    public void deleteTenantValues(String slug) {
        if (properties.githubPat() == null || properties.githubPat().isBlank()) {
            log.info("[GITHUB_PAT absent] Suppression simulée de tenants/{}/values.yaml", slug);
            return;
        }

        String path = "tenants/" + slug + "/values.yaml";
        String contentsUrl = "/repos/%s/%s/contents/%s".formatted(
                properties.githubOwner(), properties.githubRepo(), path);

        String sha = trouverShaExistant(contentsUrl);
        if (sha == null) {
            log.warn("tenants/{}/values.yaml déjà absent du repo — suppression considérée faite", slug);
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("message", "chore: suppression tenant " + slug + " (fin de vie J+60)");
        body.put("sha", sha);
        body.put("branch", "main");

        try {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(contentsUrl)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ProvisioningException(
                    "Échec de la suppression GitHub pour " + slug + " : HTTP " + e.getStatusCode(), e);
        }
    }

    @Override
    public void marquerTrialTermine(String slug) {
        if (properties.githubPat() == null || properties.githubPat().isBlank()) {
            log.info("[GITHUB_PAT absent] Fin d'essai simulée pour tenants/{}/values.yaml", slug);
            return;
        }

        String path = "tenants/" + slug + "/values.yaml";
        String contentsUrl = "/repos/%s/%s/contents/%s".formatted(
                properties.githubOwner(), properties.githubRepo(), path);

        JsonNode fichier = lireFichier(contentsUrl);
        if (fichier == null) {
            log.warn("tenants/{}/values.yaml introuvable — flip trial ignoré", slug);
            return;
        }
        // L'API Contents renvoie le contenu en base64 replié sur plusieurs
        // lignes — le décodeur MIME tolère les sauts de ligne.
        String contenu = new String(Base64.getMimeDecoder().decode(
                fichier.get("content").asText()), StandardCharsets.UTF_8);
        if (!contenu.contains("trial: true")) {
            log.info("tenants/{}/values.yaml déjà hors essai — rien à faire", slug);
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("message", "chore: fin d'essai tenant " + slug + " (abonnement actif)");
        body.put("content", Base64.getEncoder().encodeToString(
                contenu.replace("trial: true", "trial: false")
                        .getBytes(StandardCharsets.UTF_8)));
        body.put("sha", fichier.get("sha").asText());
        body.put("branch", "main");

        try {
            restClient.put()
                    .uri(contentsUrl)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ProvisioningException(
                    "Échec du flip trial pour " + slug + " : HTTP " + e.getStatusCode(), e);
        }
    }

    private String trouverShaExistant(String contentsUrl) {
        JsonNode fichier = lireFichier(contentsUrl);
        return fichier != null && fichier.has("sha") ? fichier.get("sha").asText() : null;
    }

    private JsonNode lireFichier(String contentsUrl) {
        try {
            return restClient.get()
                    .uri(contentsUrl + "?ref=main")
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null; // premier provisioning de ce tenant — pas de fichier existant
        } catch (RestClientResponseException e) {
            log.warn("Impossible de lire {} : HTTP {}", contentsUrl, e.getStatusCode());
            return null;
        }
    }
}
