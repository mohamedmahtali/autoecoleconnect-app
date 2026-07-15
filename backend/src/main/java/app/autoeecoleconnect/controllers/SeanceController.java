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

    @Operation(summary = "Lister les séances actives")
    @GetMapping
    public List<SeanceResponse> lister() {
        return seanceService.lister().stream().map(SeanceResponse::depuis).toList();
    }

    @Operation(summary = "Consulter une séance")
    @GetMapping("/{id}")
    public SeanceResponse trouver(@PathVariable UUID id) {
        return SeanceResponse.depuis(seanceService.trouver(id));
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
