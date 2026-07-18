--liquibase formatted sql

--changeset control-plane:006-organisations-stripe
-- Slice C : billing Stripe (docs/06 §6.2, colonnes différées depuis la v1.0).
-- payment_failed_at : dernier échec de paiement (dunning géré par les Smart
-- Retries Stripe — on ne fait que tracer et notifier).
ALTER TABLE organisations ADD COLUMN stripe_customer_id VARCHAR(255) UNIQUE;
ALTER TABLE organisations ADD COLUMN stripe_subscription_id VARCHAR(255);
ALTER TABLE organisations ADD COLUMN payment_failed_at TIMESTAMP;
--rollback ALTER TABLE organisations DROP COLUMN stripe_customer_id; ALTER TABLE organisations DROP COLUMN stripe_subscription_id; ALTER TABLE organisations DROP COLUMN payment_failed_at;
