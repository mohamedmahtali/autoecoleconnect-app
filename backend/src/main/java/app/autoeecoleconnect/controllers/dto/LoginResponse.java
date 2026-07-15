package app.autoeecoleconnect.controllers.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String token,
        String type,
        Instant expireLe,
        UUID id,
        String role,
        String nomComplet) {
}
