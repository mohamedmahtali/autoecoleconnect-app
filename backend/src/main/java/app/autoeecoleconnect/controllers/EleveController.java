package app.autoeecoleconnect.controllers;

import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.DonneesPersonnellesResponse;
import app.autoeecoleconnect.services.ClientService;
import app.autoeecoleconnect.services.ExamenService;
import app.autoeecoleconnect.services.ReservationService;
import app.autoeecoleconnect.services.SeanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Espace élève — droits RGPD self-service (docs/12 §12.6). Réservé au rôle
 * CLIENT par SecurityConfig ({@code GET /api/eleve/**}). L'élève n'accède
 * qu'à ses propres données : l'identifiant vient du sujet du JWT, jamais d'un
 * paramètre — il n'y a donc aucun moyen de demander les données d'un autre.
 */
@RestController
@RequestMapping("/api/eleve")
@Tag(name = "Élève — RGPD", description = "Droit d'accès de l'élève à ses propres données")
public class EleveController {

    private final ClientService clientService;
    private final ReservationService reservationService;
    private final SeanceService seanceService;
    private final ExamenService examenService;

    public EleveController(ClientService clientService, ReservationService reservationService,
                           SeanceService seanceService, ExamenService examenService) {
        this.clientService = clientService;
        this.reservationService = reservationService;
        this.seanceService = seanceService;
        this.examenService = examenService;
    }

    @Operation(summary = "Exporter mes données personnelles (droit d'accès RGPD)")
    @GetMapping("/mes-donnees")
    public DonneesPersonnellesResponse mesDonnees(@AuthenticationPrincipal Jwt jwt) {
        UUID clientId = UUID.fromString(jwt.getSubject());
        return DonneesPersonnellesResponse.depuis(
                clientService.trouver(clientId),
                reservationService.listerPourClient(clientId),
                seanceService.listerPourClient(clientId),
                examenService.listerPourClient(clientId));
    }
}
