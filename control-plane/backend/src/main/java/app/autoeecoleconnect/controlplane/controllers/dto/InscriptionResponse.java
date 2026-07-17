package app.autoeecoleconnect.controlplane.controllers.dto;

import java.util.UUID;

public record InscriptionResponse(UUID tenantId, String slug, String statut, String url) {
}
