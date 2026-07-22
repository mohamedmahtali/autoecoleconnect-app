--liquibase formatted sql

--changeset autoeecoleconnect:011-add-auto-ecole-id
-- Colonne de rattachement, ajoutée NULL d'abord : les bases en production
-- contiennent déjà des lignes, un NOT NULL immédiat échouerait. La séquence
-- imposée est ADD COLUMN NULL (ici) -> UPDATE (012) -> NOT NULL + FK (013),
-- voir docs/18 §18.3 lot 1.
-- reservations et seances portent la colonne alors qu'elles pourraient
-- déduire leur école par leurs clés étrangères (reservation -> client ->
-- auto_ecole). Dénormalisation volontaire (docs/17 §17.3) : une condition
-- locale s'indexe et ne peut pas être oubliée silencieusement, là où une
-- jointure manquante ne produit aucune erreur.
ALTER TABLE clients      ADD COLUMN auto_ecole_id UUID;
ALTER TABLE moniteurs    ADD COLUMN auto_ecole_id UUID;
ALTER TABLE voitures     ADD COLUMN auto_ecole_id UUID;
ALTER TABLE forfaits     ADD COLUMN auto_ecole_id UUID;
ALTER TABLE directeurs   ADD COLUMN auto_ecole_id UUID;
ALTER TABLE reservations ADD COLUMN auto_ecole_id UUID;
ALTER TABLE seances      ADD COLUMN auto_ecole_id UUID;
--rollback ALTER TABLE clients      DROP COLUMN auto_ecole_id;
--rollback ALTER TABLE moniteurs    DROP COLUMN auto_ecole_id;
--rollback ALTER TABLE voitures     DROP COLUMN auto_ecole_id;
--rollback ALTER TABLE forfaits     DROP COLUMN auto_ecole_id;
--rollback ALTER TABLE directeurs   DROP COLUMN auto_ecole_id;
--rollback ALTER TABLE reservations DROP COLUMN auto_ecole_id;
--rollback ALTER TABLE seances      DROP COLUMN auto_ecole_id;
