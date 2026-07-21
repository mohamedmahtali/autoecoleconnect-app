package app.autoeecoleconnect.controlplane.controllers;

import app.autoeecoleconnect.controlplane.controllers.dto.ConsolideResponse;
import app.autoeecoleconnect.controlplane.controllers.dto.MesTenantsResponse;
import app.autoeecoleconnect.controlplane.services.GerantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Gérant", description = "Dashboard lecture seule du gérant")
public class GerantController {

    private final GerantService gerantService;

    public GerantController(GerantService gerantService) {
        this.gerantService = gerantService;
    }

    @Operation(summary = "Lister les tenants de l'organisation du gérant connecté")
    @GetMapping("/mes-tenants")
    public MesTenantsResponse mesTenants(@AuthenticationPrincipal Jwt jwt) {
        // sub = id de l'organisation (voir JwtService.generer)
        return gerantService.mesTenants(UUID.fromString(jwt.getSubject()));
    }

    @Operation(summary = "Résumé consolidé (CA + élèves actifs) sur tous les tenants de l'organisation")
    @GetMapping("/mes-tenants/consolide")
    public ConsolideResponse consolide(@AuthenticationPrincipal Jwt jwt) {
        return gerantService.consolidePour(UUID.fromString(jwt.getSubject()));
    }
}
