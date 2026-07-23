package app.autoeecoleconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API stateless sécurisée par JWT (voir docs/12-securite.md §12.5).
 * Matrice d'accès :
 * - public : login, ping, catalogue forfaits en lecture, health, métriques
 *   Prometheus (scrapées en interne au cluster, jamais exposées via la
 *   Gateway publique), Swagger
 * - lecture (GET) : tout profil authentifié (contrat existant, testé —
 *   voir AuthControllerIntegrationTest#un_client_peut_lire_mais_pas_ecrire).
 *   Sur les séances et réservations, un MONITEUR/CLIENT ne voit malgré tout
 *   que les siennes — filtrage fait dans les contrôleurs, pas ici.
 *   Exception : GET /api/stats/resume est réservé à DIRECTEUR (données de
 *   chiffre d'affaires) ou au control-plane via une clé interne partagée —
 *   permitAll ici, autorisation faite à la main dans StatsController.
 * - écriture : DIRECTEUR, sauf la confirmation de sa propre séance
 *   (PATCH .../validation-moniteur réservée à MONITEUR,
 *   PATCH .../validation-client réservée à CLIENT)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ping").permitAll()
                        // Catalogue consultable sans compte (site vitrine)
                        .requestMatchers(HttpMethod.GET, "/api/forfaits", "/api/forfaits/*")
                        .permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        // Autorisation faite à la main dans StatsController (DIRECTEUR
                        // via JWT, ou control-plane via clé interne — qui n'a pas de
                        // JWT tenant à présenter, docs/16-backlog.md §16.3.A).
                        .requestMatchers(HttpMethod.GET, "/api/stats/resume").permitAll()
                        // Même raisonnement pour les routes internes : le
                        // control-plane n'a pas de JWT tenant, l'autorisation
                        // se fait dans InternalController sur le secret partagé.
                        .requestMatchers("/api/internal/**").permitAll()
                        // Auto-confirmation par le moniteur/élève de sa propre séance —
                        // avant la règle générique PATCH ci-dessous (premier match gagne).
                        .requestMatchers(HttpMethod.PATCH, "/api/seances/*/validation-moniteur")
                        .hasRole("MONITEUR")
                        .requestMatchers(HttpMethod.PATCH, "/api/seances/*/validation-client")
                        .hasRole("CLIENT")
                        // Espace élève (droit d'accès RGPD self-service) : réservé au
                        // CLIENT, qui n'y lit que ses propres données (id tiré du JWT,
                        // pas d'un paramètre — docs/12 §12.6).
                        .requestMatchers(HttpMethod.GET, "/api/eleve/**")
                        .hasRole("CLIENT")
                        // Comptes directeurs : même la lecture est réservée au
                        // DIRECTEUR — un moniteur ou un élève n'a pas à connaître
                        // la liste des responsables de l'agence (item 37).
                        .requestMatchers("/api/directeurs", "/api/directeurs/**")
                        .hasRole("DIRECTEUR")
                        // Suivi des examens : outil du directeur (item 34). La
                        // règle couvre aussi les GET, qui sinon retomberaient sur
                        // anyRequest().authenticated() et fuiteraient à un élève.
                        .requestMatchers("/api/examens", "/api/examens/**")
                        .hasRole("DIRECTEUR")
                        // Disponibilités moniteur : outil du directeur (item 35),
                        // même raisonnement que les examens (GET compris).
                        .requestMatchers("/api/disponibilites", "/api/disponibilites/**")
                        .hasRole("DIRECTEUR")
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole("DIRECTEUR")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("DIRECTEUR")
                        .requestMatchers(HttpMethod.PATCH, "/api/**").hasRole("DIRECTEUR")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("DIRECTEUR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    // Le claim "role" du JWT (DIRECTEUR, MONITEUR, CLIENT) devient
    // l'autorité Spring ROLE_<role>.
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter autorites = new JwtGrantedAuthoritiesConverter();
        autorites.setAuthoritiesClaimName("role");
        autorites.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(autorites);
        return converter;
    }
}
