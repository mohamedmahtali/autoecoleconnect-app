--liquibase formatted sql

--changeset autoeecoleconnect:013-contraintes-auto-ecole
-- Verrouillage après backfill : à partir d'ici, toute ligne créée sans école
-- est rejetée par la base. C'est le garde-fou de dernier recours, en plus du
-- filtrage applicatif (docs/18 §18.3 lot 1).
-- Les index portent sur auto_ecole_id seul : toutes les requêtes de lecture
-- filtrent désormais dessus, c'est le prédicat le plus sélectif après l'id.
ALTER TABLE clients      ALTER COLUMN auto_ecole_id SET NOT NULL;
ALTER TABLE moniteurs    ALTER COLUMN auto_ecole_id SET NOT NULL;
ALTER TABLE voitures     ALTER COLUMN auto_ecole_id SET NOT NULL;
ALTER TABLE forfaits     ALTER COLUMN auto_ecole_id SET NOT NULL;
ALTER TABLE directeurs   ALTER COLUMN auto_ecole_id SET NOT NULL;
ALTER TABLE reservations ALTER COLUMN auto_ecole_id SET NOT NULL;
ALTER TABLE seances      ALTER COLUMN auto_ecole_id SET NOT NULL;

ALTER TABLE clients      ADD CONSTRAINT fk_clients_auto_ecole      FOREIGN KEY (auto_ecole_id) REFERENCES auto_ecoles (id);
ALTER TABLE moniteurs    ADD CONSTRAINT fk_moniteurs_auto_ecole    FOREIGN KEY (auto_ecole_id) REFERENCES auto_ecoles (id);
ALTER TABLE voitures     ADD CONSTRAINT fk_voitures_auto_ecole     FOREIGN KEY (auto_ecole_id) REFERENCES auto_ecoles (id);
ALTER TABLE forfaits     ADD CONSTRAINT fk_forfaits_auto_ecole     FOREIGN KEY (auto_ecole_id) REFERENCES auto_ecoles (id);
ALTER TABLE directeurs   ADD CONSTRAINT fk_directeurs_auto_ecole   FOREIGN KEY (auto_ecole_id) REFERENCES auto_ecoles (id);
ALTER TABLE reservations ADD CONSTRAINT fk_reservations_auto_ecole FOREIGN KEY (auto_ecole_id) REFERENCES auto_ecoles (id);
ALTER TABLE seances      ADD CONSTRAINT fk_seances_auto_ecole      FOREIGN KEY (auto_ecole_id) REFERENCES auto_ecoles (id);

CREATE INDEX idx_clients_auto_ecole      ON clients (auto_ecole_id);
CREATE INDEX idx_moniteurs_auto_ecole    ON moniteurs (auto_ecole_id);
CREATE INDEX idx_voitures_auto_ecole     ON voitures (auto_ecole_id);
CREATE INDEX idx_forfaits_auto_ecole     ON forfaits (auto_ecole_id);
CREATE INDEX idx_directeurs_auto_ecole   ON directeurs (auto_ecole_id);
CREATE INDEX idx_reservations_auto_ecole ON reservations (auto_ecole_id);
CREATE INDEX idx_seances_auto_ecole      ON seances (auto_ecole_id);
--rollback DROP INDEX idx_clients_auto_ecole, idx_moniteurs_auto_ecole, idx_voitures_auto_ecole, idx_forfaits_auto_ecole, idx_directeurs_auto_ecole, idx_reservations_auto_ecole, idx_seances_auto_ecole;
--rollback ALTER TABLE clients DROP CONSTRAINT fk_clients_auto_ecole;
--rollback ALTER TABLE moniteurs DROP CONSTRAINT fk_moniteurs_auto_ecole;
--rollback ALTER TABLE voitures DROP CONSTRAINT fk_voitures_auto_ecole;
--rollback ALTER TABLE forfaits DROP CONSTRAINT fk_forfaits_auto_ecole;
--rollback ALTER TABLE directeurs DROP CONSTRAINT fk_directeurs_auto_ecole;
--rollback ALTER TABLE reservations DROP CONSTRAINT fk_reservations_auto_ecole;
--rollback ALTER TABLE seances DROP CONSTRAINT fk_seances_auto_ecole;
--rollback ALTER TABLE clients      ALTER COLUMN auto_ecole_id DROP NOT NULL;
--rollback ALTER TABLE moniteurs    ALTER COLUMN auto_ecole_id DROP NOT NULL;
--rollback ALTER TABLE voitures     ALTER COLUMN auto_ecole_id DROP NOT NULL;
--rollback ALTER TABLE forfaits     ALTER COLUMN auto_ecole_id DROP NOT NULL;
--rollback ALTER TABLE directeurs   ALTER COLUMN auto_ecole_id DROP NOT NULL;
--rollback ALTER TABLE reservations ALTER COLUMN auto_ecole_id DROP NOT NULL;
--rollback ALTER TABLE seances      ALTER COLUMN auto_ecole_id DROP NOT NULL;
