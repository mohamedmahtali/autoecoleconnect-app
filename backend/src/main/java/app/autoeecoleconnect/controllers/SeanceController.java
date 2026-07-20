package app.autoeecoleconnect.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ChangementStatutSeanceRequest;
import app.autoeecoleconnect.controllers.dto.SeanceCreationRequest;
import app.autoeecoleconnect.controllers.dto.SeanceMiseAJourRequest;
import app.autoeecoleconnect.controllers.dto.SeanceResponse;
import app.autoeecoleconnect.models.Seance;
import app.autoeecoleconnect.services.SeanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seances")
@Tag(name = "Séances", description = "Planification des séances de conduite")
public class SeanceController {

    private final SeanceService seanceService;

    public SeanceController(SeanceService seanceService) {
        this.seanceService = seanceService;
    }

    @Operation(summary = "Lister les séances actives (un moniteur ne voit que les siennes)")
    @GetMapping
    public List<SeanceResponse> lister(@AuthenticationPrincipal Jwt jwt) {
        List<Seance> seances = estMoniteur(jwt)
                ? seanceService.listerPourMoniteur(idAuthentifie(jwt))
                : seanceService.lister();
        return seances.stream().map(SeanceResponse::depuis).toList();
    }

    @Operation(summary = "Consulter une séance (un moniteur ne peut consulter que les siennes)")
    @GetMapping("/{id}")
    public SeanceResponse trouver(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Seance seance = estMoniteur(jwt)
                ? seanceService.trouverPourMoniteur(id, idAuthentifie(jwt))
                : seanceService.trouver(id);
        return SeanceResponse.depuis(seance);
    }

    @Operation(summary = "Confirmer sa présence à une séance planifiée (moniteur)")
    @PatchMapping("/{id}/validation-moniteur")
    public SeanceResponse validerParMoniteur(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return SeanceResponse.depuis(seanceService.validerParMoniteur(id, idAuthentifie(jwt)));
    }

    private boolean estMoniteur(Jwt jwt) {
        return "MONITEUR".equals(jwt.getClaimAsString("role"));
    }

    private UUID idAuthentifie(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @Operation(summary = "Planifier une séance (moniteur approuvé, créneau libre)")
    @PostMapping
    public ResponseEntity<SeanceResponse> creer(@Valid @RequestBody SeanceCreationRequest request) {
        Seance seance = seanceService.creer(request);
        return ResponseEntity
                .created(URI.create("/api/seances/" + seance.getId()))
                .body(SeanceResponse.depuis(seance));
    }

    @Operation(summary = "Reprogrammer une séance planifiée")
    @PutMapping("/{id}")
    public SeanceResponse mettreAJour(@PathVariable UUID id,
                                      @Valid @RequestBody SeanceMiseAJourRequest request) {
        return SeanceResponse.depuis(seanceService.mettreAJour(id, request));
    }

    @Operation(summary = "Clore une séance (COMPLETED, CANCELLED ou NO_SHOW)")
    @PatchMapping("/{id}/statut")
    public SeanceResponse changerStatut(@PathVariable UUID id,
                                        @Valid @RequestBody ChangementStatutSeanceRequest request) {
        return SeanceResponse.depuis(seanceService.changerStatut(id, request.statut()));
    }

    @Operation(summary = "Supprimer une séance (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        seanceService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
