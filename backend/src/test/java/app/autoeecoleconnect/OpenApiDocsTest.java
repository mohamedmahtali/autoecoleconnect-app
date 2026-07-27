package app.autoeecoleconnect;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Backlog #29 — dumpe la spec OpenAPI dans {@code target/openapi.json} pour la
 * génération des types TS du frontend (source unique de vérité). Tourne dans le
 * cycle de test normal ; le drift-check CI compare ce dump au fichier committé
 * {@code frontend/openapi.json} et échoue si l'API a bougé sans régénérer les
 * types (voir .github/workflows/ci.yml).
 */
class OpenApiDocsTest extends AbstractIntegrationTest {

    @Test
    void dumpe_la_spec_openapi() throws Exception {
        // /v3/api-docs est permitAll ; restAnonyme suffit (pas besoin du JWT).
        String brut = restAnonyme.getForObject(url("/v3/api-docs"), String.class);
        assertNotNull(brut, "spec OpenAPI absente");
        assertTrue(brut.contains("\"openapi\""), "réponse /v3/api-docs inattendue");
        assertTrue(brut.contains("\"required\""),
                "OpenApiConfig doit marquer des champs required (sinon types TS imprécis)");

        // Pretty-print déterministe → diff git lisible et stable pour le drift-check.
        ObjectMapper om = new ObjectMapper();
        String joli = om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(brut));

        Path cible = Path.of("target", "openapi.json");
        Files.createDirectories(cible.getParent());
        Files.writeString(cible, joli + "\n");
    }
}
