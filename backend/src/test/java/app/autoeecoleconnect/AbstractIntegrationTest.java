package app.autoeecoleconnect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Socle des tests d'intégration : vrai PostgreSQL (Testcontainers), migrations
 * Liquibase appliquées au démarrage. Pattern « singleton container » : démarré
 * une fois pour toute la JVM (pas de @Testcontainers/@Container, qui
 * arrêteraient le conteneur à la fin de chaque classe alors que le contexte
 * Spring, mis en cache, pointerait toujours vers l'ancien port). Ryuk le
 * supprime à la fin du run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected TestRestTemplate rest;
}
