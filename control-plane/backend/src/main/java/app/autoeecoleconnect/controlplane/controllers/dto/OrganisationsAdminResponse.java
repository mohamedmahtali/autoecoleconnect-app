package app.autoeecoleconnect.controlplane.controllers.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Vue super-admin (docs/16-backlog.md §16.3 item 17) — toutes les
// organisations de la plateforme, contrairement à MesTenantsResponse qui
// est bornée à celle du gérant connecté.
public record OrganisationsAdminResponse(List<OrganisationResume> organisations) {

    public record OrganisationResume(
            UUID id,
            String nom,
            String emailGerant,
            String statut,
            String plan,
            LocalDateTime trialEndsAt,
            List<TenantResumeAdmin> tenants) {
    }

    public record TenantResumeAdmin(String slug, String nom, String url, String statut) {
    }
}
