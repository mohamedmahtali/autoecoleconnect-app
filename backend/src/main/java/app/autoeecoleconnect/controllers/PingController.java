package app.autoeecoleconnect.controllers;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Ping", description = "Vérification de la disponibilité du backend")
public class PingController {

    @Operation(summary = "Ping du backend", description = "Répond ok si le backend est démarré")
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "status", "ok",
                "service", "autoeecoleconnect-backend",
                "timestamp", Instant.now().toString()
        );
    }
}
