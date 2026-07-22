package app.autoeecoleconnect.controlplane.controllers;

import app.autoeecoleconnect.controlplane.controllers.dto.AccesAgenceResponse;
import app.autoeecoleconnect.controlplane.controllers.dto.AutoEcoleCreationRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.AutoEcoleGerantResponse;
import app.autoeecoleconnect.controlplane.controllers.dto.ConsolideResponse;
import app.autoeecoleconnect.controlplane.controllers.dto.MesTenantsResponse;
import app.autoeecoleconnect.controlplane.config.ProvisioningProperties;
import app.autoeecoleconnect.controlplane.models.AutoEcole;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.services.AutoEcoleService;
import app.autoeecoleconnect.controlplane.services.GerantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final AutoEcoleService autoEcoleService;
    private final ProvisioningProperties properties;

    public GerantController(GerantService gerantService, AutoEcoleService autoEcoleService,
                            ProvisioningProperties properties) {
        this.gerantService = gerantService;
        this.autoEcoleService = autoEcoleService;
        this.properties = properties;
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

    @Operation(summary = "Lister les agences d'un de mes tenants")
    @GetMapping("/mes-tenants/{tenantId}/auto-ecoles")
    public List<AutoEcoleGerantResponse> agences(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable UUID tenantId) {
        Tenant tenant = autoEcoleService.exigerTenantDeLOrganisation(
                tenantId, UUID.fromString(jwt.getSubject()));
        return autoEcoleService.listerPour(tenant.getId()).stream()
                .map(agence -> AutoEcoleGerantResponse.depuis(agence, properties.domaine()))
                .toList();
    }

    @Operation(summary = "Ajouter une agence à un de mes tenants")
    @PostMapping("/mes-tenants/auto-ecoles")
    public AutoEcoleGerantResponse creerAgence(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody AutoEcoleCreationRequest request) {
        Tenant tenant = autoEcoleService.exigerTenantDeLOrganisation(
                request.tenantId(), UUID.fromString(jwt.getSubject()));
        AutoEcole agence = autoEcoleService.creer(tenant, request.nom(), request.adresse());
        return AutoEcoleGerantResponse.depuis(agence, properties.domaine());
    }

    @Operation(summary = "Ouvrir une session sur une de mes agences (sans se réauthentifier)")
    @PostMapping("/mes-tenants/auto-ecoles/{autoEcoleId}/acces")
    public AccesAgenceResponse acces(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID autoEcoleId) {
        var acces = autoEcoleService.ouvrirAcces(
                autoEcoleId,
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("nomOrganisation"),
                properties.domaine());
        return new AccesAgenceResponse(acces.token(), acces.type(), acces.url());
    }
}
