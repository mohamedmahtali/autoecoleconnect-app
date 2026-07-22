package app.autoeecoleconnect.config;

import java.io.IOException;
import java.util.UUID;

import app.autoeecoleconnect.services.ContexteAutoEcole;
import app.autoeecoleconnect.services.ResolveurAutoEcoleParHote;
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
 * <p><b>Deux sources, dans cet ordre :</b>
 * <ol>
 *   <li>le claim {@code autoEcoleId} du jeton — il fait foi, c'est lui qui
 *       porte l'autorisation ;</li>
 *   <li>à défaut, l'en-tête {@code Host} (lot 3, une URL par agence), pour
 *       les requêtes sans jeton : catalogue public des forfaits, connexion.</li>
 * </ol>
 *
 * <p>⚠️ Si le jeton désigne une agence et l'hôte une autre, <b>le jeton
 * gagne</b> : un directeur de Lyon qui ouvre l'URL de Bron continue de voir
 * Lyon. C'est déroutant mais sûr — l'inverse laisserait l'URL décider de ce
 * qu'on a le droit de lire. 🔜 Le cas disparaîtra au lot 5, quand le gérant
 * pourra changer d'agence explicitement.
 */
@Component
@Order(Integer.MAX_VALUE)
public class FiltrePerimetreAutoEcole extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltrePerimetreAutoEcole.class);

    private final ContexteAutoEcole contexteAutoEcole;
    private final ResolveurAutoEcoleParHote resolveurParHote;

    public FiltrePerimetreAutoEcole(ContexteAutoEcole contexteAutoEcole,
                                    ResolveurAutoEcoleParHote resolveurParHote) {
        this.contexteAutoEcole = contexteAutoEcole;
        this.resolveurParHote = resolveurParHote;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse,
                                    FilterChain chaine) throws ServletException, IOException {
        try {
            perimetreDuJeton()
                    .or(() -> resolveurParHote.resoudre(requete.getHeader("Host")))
                    .ifPresent(contexteAutoEcole::definir);
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
