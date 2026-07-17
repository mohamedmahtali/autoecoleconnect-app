package app.autoeecoleconnect.controlplane;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Socle des tests d'intégration : vrai PostgreSQL (Testcontainers), migrations
 * Liquibase appliquées au démarrage. Pattern « singleton container » : démarré
 * une fois pour toute la JVM (voir AbstractIntegrationTest du backend tenant
 * pour le raisonnement complet).
 *
 * <p>GITHUB_PAT/RESEND_API_KEY restent vides → GitHubService/EmailService
 * basculent automatiquement sur leurs implémentations de logging.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.provisioning.invite-token=test-invite-token")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected TestRestTemplate rest;

    @LocalServerPort
    protected int port;

    protected String url(String chemin) {
        return "http://localhost:" + port + chemin;
    }
}
