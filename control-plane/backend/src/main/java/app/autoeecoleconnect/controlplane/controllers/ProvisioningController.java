package app.autoeecoleconnect.controlplane.controllers;

import app.autoeecoleconnect.controlplane.config.ProvisioningProperties;
import app.autoeecoleconnect.controlplane.controllers.dto.InscriptionRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.InscriptionResponse;
import app.autoeecoleconnect.controlplane.exceptions.InviteTokenInvalideException;
import app.autoeecoleconnect.controlplane.services.ProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Provisioning", description = "Inscription et provisioning automatique des tenants")
public class ProvisioningController {

    private final ProvisioningService provisioningService;
    private final ProvisioningProperties properties;

    public ProvisioningController(ProvisioningService provisioningService,
                                   ProvisioningProperties properties) {
        this.provisioningService = provisioningService;
        this.properties = properties;
    }

    @Operation(summary = "Inscrire une nouvelle auto-école (provisioning automatique)")
    @PostMapping("/inscription")
    public ResponseEntity<InscriptionResponse> inscrire(
            @RequestHeader(value = "X-Invite-Token", required = false) String inviteToken,
            @Valid @RequestBody InscriptionRequest request) {

        verifierInviteToken(inviteToken);

        InscriptionResponse reponse = provisioningService.inscrire(request);
        HttpStatus statut = "failed".equals(reponse.statut()) ? HttpStatus.BAD_GATEWAY : HttpStatus.ACCEPTED;
        return ResponseEntity.status(statut).body(reponse);
    }

    // Comparaison en temps constant — évite qu'un attaquant devine le token
    // caractère par caractère via une attaque temporelle.
    private void verifierInviteToken(String inviteToken) {
        byte[] attendu = properties.inviteToken().getBytes(StandardCharsets.UTF_8);
        byte[] recu = (inviteToken == null ? "" : inviteToken).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(attendu, recu)) {
            throw new InviteTokenInvalideException();
        }
    }
}
