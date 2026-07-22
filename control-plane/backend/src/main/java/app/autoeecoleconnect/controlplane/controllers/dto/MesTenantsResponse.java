package app.autoeecoleconnect.controlplane.controllers.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MesTenantsResponse(
        String organisation,
        String statutOrganisation,
        String plan,
        LocalDateTime trialEndsAt,
        List<TenantResume> tenants) {

    /** {@code id} est necessaire au dashboard pour agir sur les agences du tenant. */
    public record TenantResume(
            UUID id,
            String slug,
            String nom,
            String url,
            String statut,
            LocalDateTime createdAt,
            LocalDateTime suspendedAt) {
    }
}
