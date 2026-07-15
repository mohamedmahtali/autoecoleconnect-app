package app.autoeecoleconnect;

import java.io.IOException;

import app.autoeecoleconnect.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Socle des tests d'intégration : vrai PostgreSQL (Testcontainers), migrations
 * Liquibase appliquées au démarrage. Pattern « singleton container » : démarré
 * une fois pour toute la JVM (pas de @Testcontainers/@Container, qui
 * arrêteraient le conteneur à la fin de chaque classe alors que le contexte
 * Spring, mis en cache, pointerait toujours vers l'ancien port). Ryuk le
 * supprime à la fin du run.
 *
 * <p>{@code rest} est authentifié en DIRECTEUR (compte bootstrap) ; pour un
 * appel anonyme ou avec un autre profil, utiliser {@link #restAnonyme} avec
 * {@link #url(String)} et un header Authorization explicite.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@autoecoleconnect.local";
    protected static final String ADMIN_PASSWORD = "changez-moi-en-production";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    private static volatile String tokenAdmin;

    @Autowired
    protected TestRestTemplate rest;

    @LocalServerPort
    protected int port;

    protected final TestRestTemplate restAnonyme = new TestRestTemplate();

    protected String url(String chemin) {
        return "http://localhost:" + port + chemin;
    }

    @BeforeEach
    void authentifierEnDirecteur() {
        if (tokenAdmin == null) {
            LoginResponse reponse = rest.postForEntity("/api/auth/login",
                    new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD),
                    LoginResponse.class).getBody();
            tokenAdmin = reponse.token();
        }
        boolean dejaInstalle = rest.getRestTemplate().getInterceptors().stream()
                .anyMatch(BearerAdminInterceptor.class::isInstance);
        if (!dejaInstalle) {
            rest.getRestTemplate().getInterceptors().add(new BearerAdminInterceptor());
        }
    }

    // Ajoute le token directeur à chaque requête qui n'a pas déjà son propre
    // header Authorization.
    private static final class BearerAdminInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION) && tokenAdmin != null) {
                request.getHeaders().setBearerAuth(tokenAdmin);
            }
            return execution.execute(request, body);
        }
    }
}
