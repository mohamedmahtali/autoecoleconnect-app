package app.autoeecoleconnect.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ChangementStatutMoniteurRequest;
import app.autoeecoleconnect.controllers.dto.MoniteurCreationRequest;
import app.autoeecoleconnect.controllers.dto.MoniteurMiseAJourRequest;
import app.autoeecoleconnect.controllers.dto.MoniteurResponse;
import app.autoeecoleconnect.models.Moniteur;
import app.autoeecoleconnect.services.MoniteurService;
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
@RequestMapping("/api/moniteurs")
@Tag(name = "Moniteurs", description = "Gestion des moniteurs et de leur workflow d'approbation")
public class MoniteurController {

    private final MoniteurService moniteurService;

    public MoniteurController(MoniteurService moniteurService) {
        this.moniteurService = moniteurService;
    }

    @Operation(summary = "Lister les moniteurs actifs")
    @GetMapping
    public List<MoniteurResponse> lister() {
        return moniteurService.lister().stream().map(MoniteurResponse::depuis).toList();
    }

    @Operation(summary = "Consulter un moniteur")
    @GetMapping("/{id}")
    public MoniteurResponse trouver(@PathVariable UUID id) {
        return MoniteurResponse.depuis(moniteurService.trouver(id));
    }

    @Operation(summary = "Créer un moniteur (statut initial PENDING)")
    @PostMapping
    public ResponseEntity<MoniteurResponse> creer(
            @Valid @RequestBody MoniteurCreationRequest request) {
        Moniteur moniteur = moniteurService.creer(request);
        return ResponseEntity
                .created(URI.create("/api/moniteurs/" + moniteur.getId()))
                .body(MoniteurResponse.depuis(moniteur));
    }

    @Operation(summary = "Mettre à jour un moniteur")
    @PutMapping("/{id}")
    public MoniteurResponse mettreAJour(@PathVariable UUID id,
                                        @Valid @RequestBody MoniteurMiseAJourRequest request) {
        return MoniteurResponse.depuis(moniteurService.mettreAJour(id, request));
    }

    @Operation(summary = "Changer le statut (PENDING → APPROVED/REJECTED, APPROVED ↔ INACTIVE)")
    @PatchMapping("/{id}/statut")
    public MoniteurResponse changerStatut(@PathVariable UUID id,
                                          @Valid @RequestBody ChangementStatutMoniteurRequest request) {
        return MoniteurResponse.depuis(moniteurService.changerStatut(id, request.statut()));
    }

    @Operation(summary = "Désactiver un moniteur (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        moniteurService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
