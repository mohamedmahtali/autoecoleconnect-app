package app.autoeecoleconnect.controlplane.controllers;

import app.autoeecoleconnect.controlplane.services.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/abonnement")
@Tag(name = "Abonnement", description = "Souscription Stripe (gérant)")
public class AbonnementController {

    private final CheckoutService checkoutService;

    public AbonnementController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Operation(summary = "Créer une session Stripe Checkout pour le plan de l'organisation")
    @PostMapping("/checkout")
    public Map<String, String> checkout(@AuthenticationPrincipal Jwt jwt) {
        String url = checkoutService.creerSessionCheckout(UUID.fromString(jwt.getSubject()));
        return Map.of("url", url);
    }
}
