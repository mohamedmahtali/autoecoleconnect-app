package app.autoeecoleconnect.controllers;

import app.autoeecoleconnect.controllers.dto.StatsResponse;
import app.autoeecoleconnect.services.AuthentificationInterne;
import app.autoeecoleconnect.services.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

// docs/16-backlog.md §16.3, fondation partagée items 14/15. Route en
// permitAll côté SecurityConfig — l'autorisation se fait ici à la main,
// parce que deux appelants très différents doivent y accéder : un DIRECTEUR
// via JWT normal, et le control-plane (aucun JWT tenant) via un secret
// partagé (docs/16-backlog.md §16.3.A).
@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistiques", description = "KPI d'activité de l'auto-école")
public class StatsController {

    private final StatsService statsService;
    private final AuthentificationInterne authentificationInterne;

    public StatsController(StatsService statsService,
                           AuthentificationInterne authentificationInterne) {
        this.statsService = statsService;
        this.authentificationInterne = authentificationInterne;
    }

    @Operation(summary = "Résumé chiffré de l'activité (DIRECTEUR, ou control-plane via clé interne)")
    @GetMapping("/resume")
    public StatsResponse resume(@AuthenticationPrincipal Jwt jwt,
                                @RequestHeader(value = "X-Internal-Api-Key", required = false) String cleAppelant) {
        if (!authentificationInterne.estValide(cleAppelant) && !estDirecteur(jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return statsService.resume();
    }

    private boolean estDirecteur(Jwt jwt) {
        return jwt != null && "DIRECTEUR".equals(jwt.getClaimAsString("role"));
    }
}
