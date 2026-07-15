package app.autoeecoleconnect.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ForfaitRequest;
import app.autoeecoleconnect.controllers.dto.ForfaitResponse;
import app.autoeecoleconnect.models.Forfait;
import app.autoeecoleconnect.services.ForfaitService;
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
@RequestMapping("/api/forfaits")
@Tag(name = "Forfaits", description = "Catalogue des forfaits de conduite")
public class ForfaitController {

    private final ForfaitService forfaitService;

    public ForfaitController(ForfaitService forfaitService) {
        this.forfaitService = forfaitService;
    }

    @Operation(summary = "Lister les forfaits actifs")
    @GetMapping
    public List<ForfaitResponse> lister() {
        return forfaitService.lister().stream().map(ForfaitResponse::depuis).toList();
    }

    @Operation(summary = "Consulter un forfait")
    @GetMapping("/{id}")
    public ForfaitResponse trouver(@PathVariable UUID id) {
        return ForfaitResponse.depuis(forfaitService.trouver(id));
    }

    @Operation(summary = "Créer un forfait")
    @PostMapping
    public ResponseEntity<ForfaitResponse> creer(@Valid @RequestBody ForfaitRequest request) {
        Forfait forfait = forfaitService.creer(request);
        return ResponseEntity
                .created(URI.create("/api/forfaits/" + forfait.getId()))
                .body(ForfaitResponse.depuis(forfait));
    }

    @Operation(summary = "Mettre à jour un forfait")
    @PutMapping("/{id}")
    public ForfaitResponse mettreAJour(@PathVariable UUID id,
                                       @Valid @RequestBody ForfaitRequest request) {
        return ForfaitResponse.depuis(forfaitService.mettreAJour(id, request));
    }

    @Operation(summary = "Retirer un forfait du catalogue (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        forfaitService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
