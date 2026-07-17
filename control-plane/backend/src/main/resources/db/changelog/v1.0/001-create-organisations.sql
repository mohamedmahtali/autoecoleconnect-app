--liquibase formatted sql

--changeset control-plane:001-create-organisations
-- Version allégée de docs/06 §6.2 : les colonnes Stripe (stripe_customer_id,
-- stripe_subscription_id, reminder_sent) sont différées à la Slice B, pas
-- utilisées tant que le billing n'existe pas.
CREATE TABLE organisations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom            VARCHAR(255) NOT NULL,
    email_gerant   VARCHAR(255) NOT NULL UNIQUE,
    plan           VARCHAR(50)  NOT NULL DEFAULT 'solo',
                   -- solo | pro | groupe | reseau
    statut         VARCHAR(50)  NOT NULL DEFAULT 'trial',
                   -- trial | active | suspended | deleted
    trial_ends_at  TIMESTAMP    NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE organisations;
