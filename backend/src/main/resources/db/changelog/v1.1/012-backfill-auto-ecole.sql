--liquibase formatted sql

--changeset autoeecoleconnect:012-backfill-auto-ecole
-- Rattachement des données existantes à une auto-école par défaut, qui
-- reprend le slug du tenant : avant cette refonte, le tenant EST l'école, la
-- correspondance est donc exacte. Le gérant pourra la renommer ensuite.
-- ${tenantSlug} vient de spring.liquibase.parameters (application.yml),
-- alimenté par la variable d'environnement TENANT_SLUG que le chart
-- portail-tenant injecte déjà.
-- Volontairement séparé du DDL pour pouvoir être rejoué seul si un rattachement
-- doit être repris (docs/18 §18.3 lot 1).
INSERT INTO auto_ecoles (nom, slug)
SELECT '${tenantSlug}', '${tenantSlug}'
WHERE NOT EXISTS (SELECT 1 FROM auto_ecoles);

UPDATE clients      SET auto_ecole_id = (SELECT id FROM auto_ecoles ORDER BY created_at LIMIT 1) WHERE auto_ecole_id IS NULL;
UPDATE moniteurs    SET auto_ecole_id = (SELECT id FROM auto_ecoles ORDER BY created_at LIMIT 1) WHERE auto_ecole_id IS NULL;
UPDATE voitures     SET auto_ecole_id = (SELECT id FROM auto_ecoles ORDER BY created_at LIMIT 1) WHERE auto_ecole_id IS NULL;
UPDATE forfaits     SET auto_ecole_id = (SELECT id FROM auto_ecoles ORDER BY created_at LIMIT 1) WHERE auto_ecole_id IS NULL;
UPDATE directeurs   SET auto_ecole_id = (SELECT id FROM auto_ecoles ORDER BY created_at LIMIT 1) WHERE auto_ecole_id IS NULL;
UPDATE reservations SET auto_ecole_id = (SELECT id FROM auto_ecoles ORDER BY created_at LIMIT 1) WHERE auto_ecole_id IS NULL;
UPDATE seances      SET auto_ecole_id = (SELECT id FROM auto_ecoles ORDER BY created_at LIMIT 1) WHERE auto_ecole_id IS NULL;
--rollback UPDATE clients      SET auto_ecole_id = NULL;
--rollback UPDATE moniteurs    SET auto_ecole_id = NULL;
--rollback UPDATE voitures     SET auto_ecole_id = NULL;
--rollback UPDATE forfaits     SET auto_ecole_id = NULL;
--rollback UPDATE directeurs   SET auto_ecole_id = NULL;
--rollback UPDATE reservations SET auto_ecole_id = NULL;
--rollback UPDATE seances      SET auto_ecole_id = NULL;
--rollback DELETE FROM auto_ecoles;
