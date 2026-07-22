package app.autoeecoleconnect.config;

import java.io.IOException;
import java.util.UUID;

import app.autoeecoleconnect.services.ContexteAutoEcole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Pose le périmètre de la requête dans {@link ContexteAutoEcole} à partir du
 * claim {@code autoEcoleId} du JWT (docs/18 §18.3 lot 2). C'est le seul
 * endroit qui alimente ce contexte côté web — tout le filtrage des services
 * en dépend.
 *
 * <p>S'exécute <b>après</b> la chaîne de sécurité Spring, pour que
 * l'authentification soit déjà résolue : d'où l'{@code @Order} élevé.
 *
 * <p>🔜 Au lot 3, l'en-tête {@code Host} deviendra une seconde source (une
 * URL par agence) et le gérant, qui n'a pas d'agence unique, pourra choisir
 * laquelle consulter.
 */
@Component
@Order(Integer.MAX_VALUE)
public class FiltrePerimetreAutoEcole extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltrePerimetreAutoEcole.class);

    private final ContexteAutoEcole contexteAutoEcole;

    public FiltrePerimetreAutoEcole(ContexteAutoEcole contexteAutoEcole) {
        this.contexteAutoEcole = contexteAutoEcole;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse,
                                    FilterChain chaine) throws ServletException, IOException {
        try {
            perimetreDuJeton().ifPresent(contexteAutoEcole::definir);
            chaine.doFilter(requete, reponse);
        } finally {
            // Impératif : les threads du conteneur sont recyclés d'une requête
            // à l'autre. Un périmètre laissé en place fuiterait sur la requête
            // suivante, potentiellement celle d'une autre agence.
            contexteAutoEcole.effacer();
        }
    }

    private java.util.Optional<UUID> perimetreDuJeton() {
        var authentification = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentification instanceof JwtAuthenticationToken jeton)) {
            return java.util.Optional.empty();
        }
        Jwt jwt = jeton.getToken();
        String brut = jwt.getClaimAsString("autoEcoleId");
        if (brut == null || brut.isBlank()) {
            // Jeton émis avant l'ajout du claim : on laisse l'agence par défaut
            // s'appliquer plutôt que de rejeter la requête. Le filtrage reste
            // effectif, il n'est simplement pas piloté par le porteur.
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(UUID.fromString(brut));
        } catch (IllegalArgumentException e) {
            log.warn("Claim autoEcoleId illisible dans le jeton : {}", brut);
            return java.util.Optional.empty();
        }
    }
}
