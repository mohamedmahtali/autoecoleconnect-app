package app.autoeecoleconnect.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.DisponibiliteCreationRequest;
import app.autoeecoleconnect.controllers.dto.DisponibiliteResponse;
import app.autoeecoleconnect.models.DisponibiliteMoniteur;
import app.autoeecoleconnect.services.DisponibiliteMoniteurService;
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

// Créneaux de disponibilité récurrents des moniteurs (backlog #35). Réservé au
// DIRECTEUR par SecurityConfig (/api/disponibilites/**). Créneaux ajoutés et
// supprimés (pas de mise à jour : on supprime et on recrée).
@RestController
@RequestMapping("/api/disponibilites")
@Tag(name = "Disponibilités moniteur", description = "Créneaux récurrents alimentant le taux d'occupation")
public class DisponibiliteMoniteurController {

    private final DisponibiliteMoniteurService disponibiliteService;

    public DisponibiliteMoniteurController(DisponibiliteMoniteurService disponibiliteService) {
        this.disponibiliteService = disponibiliteService;
    }

    @Operation(summary = "Lister les créneaux de disponibilité de l'agence")
    @GetMapping
    public List<DisponibiliteResponse> lister() {
        return disponibiliteService.lister().stream().map(DisponibiliteResponse::depuis).toList();
    }

    @Operation(summary = "Ajouter un créneau récurrent à un moniteur")
    @PostMapping
    public ResponseEntity<DisponibiliteResponse> creer(
            @Valid @RequestBody DisponibiliteCreationRequest request) {
        DisponibiliteMoniteur disponibilite = disponibiliteService.creer(request);
        return ResponseEntity
                .created(URI.create("/api/disponibilites/" + disponibilite.getId()))
                .body(DisponibiliteResponse.depuis(disponibilite));
    }

    @Operation(summary = "Supprimer un créneau (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        disponibiliteService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
