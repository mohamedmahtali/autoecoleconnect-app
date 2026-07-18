package app.autoeecoleconnect.controlplane.controllers.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String token,
        String type,
        Instant expireLe,
        UUID organisationId,
        String nomOrganisation) {
}
