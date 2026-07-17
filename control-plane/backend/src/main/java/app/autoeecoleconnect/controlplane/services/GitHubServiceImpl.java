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

    private String trouverShaExistant(String contentsUrl) {
        try {
            JsonNode response = restClient.get()
                    .uri(contentsUrl + "?ref=main")
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && response.has("sha") ? response.get("sha").asText() : null;
        } catch (HttpClientErrorException.NotFound e) {
            return null; // premier provisioning de ce tenant — pas de fichier existant
        } catch (RestClientResponseException e) {
            log.warn("Impossible de vérifier l'existence de {} avant commit : HTTP {}",
                    contentsUrl, e.getStatusCode());
            return null;
        }
    }
}
