package app.autoeecoleconnect.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ExamenCreationRequest;
import app.autoeecoleconnect.controllers.dto.ExamenMiseAJourRequest;
import app.autoeecoleconnect.controllers.dto.ExamenResponse;
import app.autoeecoleconnect.models.Examen;
import app.autoeecoleconnect.services.ExamenService;
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

// Suivi des examens (backlog #34). Réservé au DIRECTEUR par SecurityConfig
// (/api/examens/** → hasRole DIRECTEUR).
@RestController
@RequestMapping("/api/examens")
@Tag(name = "Examens", description = "Suivi des passages d'examen (code/conduite) des élèves")
public class ExamenController {

    private final ExamenService examenService;

    public ExamenController(ExamenService examenService) {
        this.examenService = examenService;
    }

    @Operation(summary = "Lister les examens de l'agence")
    @GetMapping
    public List<ExamenResponse> lister() {
        return examenService.lister().stream().map(ExamenResponse::depuis).toList();
    }

    @Operation(summary = "Consulter un examen")
    @GetMapping("/{id}")
    public ExamenResponse trouver(@PathVariable UUID id) {
        return ExamenResponse.depuis(examenService.trouver(id));
    }

    @Operation(summary = "Enregistrer un examen (convocation, ou résultat déjà connu)")
    @PostMapping
    public ResponseEntity<ExamenResponse> creer(@Valid @RequestBody ExamenCreationRequest request) {
        Examen examen = examenService.creer(request);
        return ResponseEntity
                .created(URI.create("/api/examens/" + examen.getId()))
                .body(ExamenResponse.depuis(examen));
    }

    @Operation(summary = "Mettre à jour un examen (typiquement pour saisir le résultat)")
    @PutMapping("/{id}")
    public ExamenResponse mettreAJour(@PathVariable UUID id,
                                      @Valid @RequestBody ExamenMiseAJourRequest request) {
        return ExamenResponse.depuis(examenService.mettreAJour(id, request));
    }

    @Operation(summary = "Supprimer un examen (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        examenService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
