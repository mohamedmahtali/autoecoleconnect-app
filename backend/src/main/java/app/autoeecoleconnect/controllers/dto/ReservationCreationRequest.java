package app.autoeecoleconnect.controllers.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import app.autoeecoleconnect.models.PaiementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// dateFin est calculée depuis la validité du forfait ; montant vaut par
// défaut le prix du forfait (surchargeable pour une remise).
public record ReservationCreationRequest(
        @NotNull UUID clientId,
        @NotNull UUID forfaitId,
        @NotNull LocalDate dateDebut,
        @Positive BigDecimal montant,
        PaiementType paiementType,
        String notes) {
}
