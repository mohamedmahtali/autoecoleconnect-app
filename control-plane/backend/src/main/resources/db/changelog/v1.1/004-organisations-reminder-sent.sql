--liquibase formatted sql

--changeset control-plane:004-organisations-reminder-sent
-- Slice B : évite le double envoi du rappel de fin d'essai (docs/09 §9.3) —
-- remis à false uniquement si un futur renouvellement de trial l'exige (Slice C).
ALTER TABLE organisations ADD COLUMN reminder_sent BOOLEAN NOT NULL DEFAULT false;
--rollback ALTER TABLE organisations DROP COLUMN reminder_sent;
