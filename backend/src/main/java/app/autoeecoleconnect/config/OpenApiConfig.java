package app.autoeecoleconnect.config;

import java.util.ArrayList;

import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI autoEcoleConnectOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AutoEcoleConnect API")
                        .description("API backend de la plateforme SaaS multi-tenant pour auto-écoles")
                        .version("v0.1.0"))
                // Bouton « Authorize » : coller le token renvoyé par /api/auth/login
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
     * Backlog #29 — spec précise pour la génération des types TS. Par défaut
     * springdoc marque toutes les propriétés comme optionnelles ({@code id?}) ;
     * or nos DTO de réponse sont des records dont chaque composant est toujours
     * présent dans le JSON. On force donc {@code required} sur toutes les
     * propriétés. La nullabilité est portée à part par {@code @Schema(nullable)}
     * sur les composants concernés → openapi-typescript rend {@code T | null}.
     */
    @Bean
    public OpenApiCustomizer proprietesRequisesParDefaut() {
        return openApi -> {
            var composants = openApi.getComponents();
            if (composants == null || composants.getSchemas() == null) {
                return;
            }
            composants.getSchemas().values().forEach(this::marquerToutRequis);
        };
    }

    private void marquerToutRequis(Schema<?> schema) {
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        }
    }
}
