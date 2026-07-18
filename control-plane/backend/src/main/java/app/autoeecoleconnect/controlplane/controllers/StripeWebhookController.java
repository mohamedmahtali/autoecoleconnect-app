package app.autoeecoleconnect.controlplane.controllers;

import app.autoeecoleconnect.controlplane.config.StripeProperties;
import app.autoeecoleconnect.controlplane.services.ActivationService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réception des webhooks Stripe. Endpoint public (permitAll) : la sécurité est
 * la signature HMAC du header Stripe-Signature, vérifiée sur le corps BRUT
 * (@RequestBody String — Spring ne re-sérialise pas une String).
 */
@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Webhooks Stripe (billing)")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final ActivationService activationService;
    private final StripeProperties properties;

    public StripeWebhookController(ActivationService activationService,
                                    StripeProperties properties) {
        this.activationService = activationService;
        this.properties = properties;
    }

    @Operation(summary = "Recevoir un événement Stripe (signature vérifiée)")
    @PostMapping("/stripe")
    public ResponseEntity<Void> recevoir(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, signature == null ? "" : signature,
                    properties.webhookSecret() == null ? "" : properties.webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Webhook Stripe rejeté : signature invalide");
            return ResponseEntity.badRequest().build();
        }

        activationService.traiter(event);
        return ResponseEntity.ok().build();
    }
}
