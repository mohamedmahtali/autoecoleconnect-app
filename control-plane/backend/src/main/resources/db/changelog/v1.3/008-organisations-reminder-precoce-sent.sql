--liquibase formatted sql

--changeset control-plane:008-organisations-reminder-precoce-sent
-- docs/16-backlog.md §16.3 item 16 : rappel précoce J-25, distinct de
-- reminder_sent (J-5) pour ne pas empêcher ce dernier de partir.
ALTER TABLE organisations ADD COLUMN reminder_precoce_sent BOOLEAN NOT NULL DEFAULT false;
--rollback ALTER TABLE organisations DROP COLUMN reminder_precoce_sent;
