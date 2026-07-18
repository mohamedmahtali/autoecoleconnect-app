package app.autoeecoleconnect.controlplane.controllers.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MesTenantsResponse(
        String organisation,
        String statutOrganisation,
        String plan,
        LocalDateTime trialEndsAt,
        List<TenantResume> tenants) {

    public record TenantResume(
            String slug,
            String nom,
            String url,
            String statut,
            LocalDateTime createdAt,
            LocalDateTime suspendedAt) {
    }
}
