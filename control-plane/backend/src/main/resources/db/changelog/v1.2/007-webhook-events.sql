--liquibase formatted sql

--changeset control-plane:007-webhook-events
-- Idempotence des webhooks Stripe (docs/06 §6.2) : un event déjà inséré ne
-- doit jamais être retraité (Stripe livre au-moins-une-fois).
CREATE TABLE webhook_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE webhook_events;
