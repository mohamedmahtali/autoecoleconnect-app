--liquibase formatted sql

--changeset control-plane:005-organisations-mot-de-passe-gerant
-- Slice B : login gérant sur le Control Plane. Nullable : les organisations
-- créées avant cette migration n'ont pas de mot de passe — leur login est
-- refusé tant qu'aucun hash n'est défini (voir AuthService).
ALTER TABLE organisations ADD COLUMN mot_de_passe_hash VARCHAR(255);
--rollback ALTER TABLE organisations DROP COLUMN mot_de_passe_hash;
