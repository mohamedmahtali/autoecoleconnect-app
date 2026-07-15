--liquibase formatted sql

--changeset autoeecoleconnect:007-create-reservations
CREATE TABLE reservations (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id                UUID          NOT NULL REFERENCES clients(id),
    forfait_id               UUID          NOT NULL REFERENCES forfaits(id),
    date_debut               DATE          NOT NULL,
    date_fin                 DATE          NOT NULL,
    date_reservation         TIMESTAMP     NOT NULL DEFAULT NOW(),
    montant                  DECIMAL(10,2) NOT NULL,
    paiement_type            VARCHAR(50),
                             -- STRIPE | PAYPLUG | ALMA | ESPECE | CHEQUE
                             -- VIREMENT | CPF | PERMIS1EURO
    paiement_statut          VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
                             -- PENDING | PAID | REFUNDED | FAILED
    stripe_payment_intent_id VARCHAR(255),
    statut                   VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
                             -- PENDING | ACTIVE | COMPLETED | CANCELLED | EXPIRED
    notes                    TEXT,
    active                   BOOLEAN       NOT NULL DEFAULT true,
    created_at               TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservations_client ON reservations (client_id);
CREATE INDEX idx_reservations_forfait ON reservations (forfait_id);
--rollback DROP TABLE reservations;
