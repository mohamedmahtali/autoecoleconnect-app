package app.autoeecoleconnect.controllers.dto;

import app.autoeecoleconnect.models.PaiementType;
import jakarta.validation.constraints.NotNull;

// Enregistrement d'un paiement hors Stripe (espèces/chèque/virement/CPF) —
// docs/16-backlog.md §16.2 item 9. reference est libre (n° de chèque,
// référence de virement...), ajoutée aux notes de la réservation : pas de
// colonne dédiée en base (voir docs/06 §6.3, jamais construite).
public record PaiementManuelRequest(
        @NotNull PaiementType paiementType,
        String reference) {
}
