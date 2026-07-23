--liquibase formatted sql

--changeset autoeecoleconnect:015-create-disponibilites-moniteur
-- Creneaux de disponibilite recurrents des moniteurs (backlog #35). Table
-- neuve, donc auto_ecole_id NOT NULL + cle etrangere des le depart.
CREATE TABLE disponibilites_moniteur (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moniteur_id   UUID        NOT NULL REFERENCES moniteurs(id),
    auto_ecole_id UUID        NOT NULL REFERENCES auto_ecoles(id),
    jour          VARCHAR(20) NOT NULL,
                  -- LUNDI | MARDI | MERCREDI | JEUDI | VENDREDI | SAMEDI | DIMANCHE
    heure_debut   TIME        NOT NULL,
    heure_fin     TIME        NOT NULL,
    active        BOOLEAN     NOT NULL DEFAULT true,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dispos_moniteur ON disponibilites_moniteur (moniteur_id);
CREATE INDEX idx_dispos_auto_ecole ON disponibilites_moniteur (auto_ecole_id);
--rollback DROP TABLE disponibilites_moniteur;
