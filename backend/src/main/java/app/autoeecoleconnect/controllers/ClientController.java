package app.autoeecoleconnect.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientMiseAJourRequest;
import app.autoeecoleconnect.controllers.dto.ClientResponse;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.services.ClientService;
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
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Gestion des élèves de l'auto-école")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Operation(summary = "Lister les clients actifs")
    @GetMapping
    public List<ClientResponse> lister() {
        return clientService.lister().stream().map(ClientResponse::depuis).toList();
    }

    @Operation(summary = "Consulter un client")
    @GetMapping("/{id}")
    public ClientResponse trouver(@PathVariable UUID id) {
        return ClientResponse.depuis(clientService.trouver(id));
    }

    @Operation(summary = "Créer un client")
    @PostMapping
    public ResponseEntity<ClientResponse> creer(@Valid @RequestBody ClientCreationRequest request) {
        Client client = clientService.creer(request);
        return ResponseEntity
                .created(URI.create("/api/clients/" + client.getId()))
                .body(ClientResponse.depuis(client));
    }

    @Operation(summary = "Mettre à jour un client")
    @PutMapping("/{id}")
    public ClientResponse mettreAJour(@PathVariable UUID id,
                                      @Valid @RequestBody ClientMiseAJourRequest request) {
        return ClientResponse.depuis(clientService.mettreAJour(id, request));
    }

    @Operation(summary = "Désactiver un client (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        clientService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Anonymiser un élève à sa demande (droit à l'effacement RGPD — "
            + "irréversible : données personnelles effacées, login rendu impossible, "
            + "réservations/séances conservées pour l'historique comptable)")
    @PostMapping("/{id}/anonymisation")
    public ClientResponse anonymiser(@PathVariable UUID id) {
        return ClientResponse.depuis(clientService.anonymiser(id));
    }
}
