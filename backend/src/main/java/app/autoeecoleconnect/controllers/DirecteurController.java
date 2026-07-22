package app.autoeecoleconnect.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.DirecteurCreationRequest;
import app.autoeecoleconnect.controllers.dto.DirecteurResponse;
import app.autoeecoleconnect.models.Directeur;
import app.autoeecoleconnect.services.DirecteurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des directeurs d'une agence (docs/16-backlog.md item 37) — permet
 * de nommer un directeur salarié ou de remplacer celui qui part, ce qui
 * n'avait aucune solution avant.
 *
 * <p>Réservé au rôle DIRECTEUR par {@code SecurityConfig} : un moniteur ou un
 * élève n'a pas à connaître la liste des directeurs.
 */
@RestController
@RequestMapping("/api/directeurs")
@Tag(name = "Directeurs", description = "Comptes directeurs de l'agence")
public class DirecteurController {

    private final DirecteurService directeurService;

    public DirecteurController(DirecteurService directeurService) {
        this.directeurService = directeurService;
    }

    @Operation(summary = "Lister les directeurs de l'agence")
    @GetMapping
    public List<DirecteurResponse> lister() {
        return directeurService.lister().stream().map(DirecteurResponse::depuis).toList();
    }

    @Operation(summary = "Consulter un directeur")
    @GetMapping("/{id}")
    public DirecteurResponse trouver(@PathVariable UUID id) {
        return DirecteurResponse.depuis(directeurService.trouver(id));
    }

    @Operation(summary = "Créer un compte directeur dans l'agence courante")
    @PostMapping
    public ResponseEntity<DirecteurResponse> creer(@Valid @RequestBody DirecteurCreationRequest request) {
        Directeur directeur = directeurService.creer(request);
        return ResponseEntity
                .created(URI.create("/api/directeurs/" + directeur.getId()))
                .body(DirecteurResponse.depuis(directeur));
    }

    @Operation(summary = "Désactiver un directeur (refusé s'il est le dernier de l'agence)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        directeurService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
