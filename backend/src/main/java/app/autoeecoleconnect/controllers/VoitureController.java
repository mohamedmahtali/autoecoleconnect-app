package app.autoeecoleconnect.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.VoitureRequest;
import app.autoeecoleconnect.controllers.dto.VoitureResponse;
import app.autoeecoleconnect.models.Voiture;
import app.autoeecoleconnect.services.VoitureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voitures")
@Tag(name = "Voitures", description = "Gestion du parc de véhicules")
public class VoitureController {

    private final VoitureService voitureService;

    public VoitureController(VoitureService voitureService) {
        this.voitureService = voitureService;
    }

    @Operation(summary = "Lister les véhicules actifs")
    @GetMapping
    public List<VoitureResponse> lister() {
        return voitureService.lister().stream().map(VoitureResponse::depuis).toList();
    }

    @Operation(summary = "Consulter un véhicule")
    @GetMapping("/{id}")
    public VoitureResponse trouver(@PathVariable UUID id) {
        return VoitureResponse.depuis(voitureService.trouver(id));
    }

    @Operation(summary = "Ajouter un véhicule au parc")
    @PostMapping
    public ResponseEntity<VoitureResponse> creer(@Valid @RequestBody VoitureRequest request) {
        Voiture voiture = voitureService.creer(request);
        return ResponseEntity
                .created(URI.create("/api/voitures/" + voiture.getId()))
                .body(VoitureResponse.depuis(voiture));
    }

    @Operation(summary = "Mettre à jour un véhicule")
    @PutMapping("/{id}")
    public VoitureResponse mettreAJour(@PathVariable UUID id,
                                       @Valid @RequestBody VoitureRequest request) {
        return VoitureResponse.depuis(voitureService.mettreAJour(id, request));
    }

    @Operation(summary = "Retirer un véhicule du parc (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        voitureService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
