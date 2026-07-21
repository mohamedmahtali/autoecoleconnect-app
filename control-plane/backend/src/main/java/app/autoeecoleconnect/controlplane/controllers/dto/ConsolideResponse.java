package app.autoeecoleconnect.controlplane.controllers.dto;

import java.math.BigDecimal;

// Dashboard consolidé multi-agences (docs/16-backlog.md §16.3 item 14).
// tenantsInterroges/tenantsEnErreur permettent de distinguer un vrai zéro
// d'un résumé partiel (un ou plusieurs tenants injoignables).
public record ConsolideResponse(
        BigDecimal caTotal,
        long elevesActifs,
        int tenantsInterroges,
        int tenantsEnErreur) {
}
