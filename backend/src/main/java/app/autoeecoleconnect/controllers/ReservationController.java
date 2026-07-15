package app.autoeecoleconnect.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ReservationCreationRequest;
import app.autoeecoleconnect.controllers.dto.ReservationResponse;
import app.autoeecoleconnect.models.Reservation;
import app.autoeecoleconnect.services.ReservationService;
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

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations", description = "Réservations de forfaits par les élèves")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Lister les réservations actives")
    @GetMapping
    public List<ReservationResponse> lister() {
        return reservationService.lister().stream().map(ReservationResponse::depuis).toList();
    }

    @Operation(summary = "Consulter une réservation")
    @GetMapping("/{id}")
    public ReservationResponse trouver(@PathVariable UUID id) {
        return ReservationResponse.depuis(reservationService.trouver(id));
    }

    @Operation(summary = "Créer une réservation (dateFin et montant déduits du forfait)")
    @PostMapping
    public ResponseEntity<ReservationResponse> creer(
            @Valid @RequestBody ReservationCreationRequest request) {
        Reservation reservation = reservationService.creer(request);
        return ResponseEntity
                .created(URI.create("/api/reservations/" + reservation.getId()))
                .body(ReservationResponse.depuis(reservation));
    }

    @Operation(summary = "Annuler une réservation (PENDING ou ACTIVE uniquement)")
    @PostMapping("/{id}/annulation")
    public ReservationResponse annuler(@PathVariable UUID id) {
        return ReservationResponse.depuis(reservationService.annuler(id));
    }

    @Operation(summary = "Supprimer une réservation (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        reservationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
