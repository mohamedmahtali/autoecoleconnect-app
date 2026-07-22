--liquibase formatted sql

--changeset autoeecoleconnect:010-create-auto-ecoles
-- Refonte du grain de tenancy (docs/17 et docs/18) : une base = une
-- organisation, qui contient une ou plusieurs auto-écoles. Le slug est
-- globalement unique côté control-plane (SlugService), il sert de
-- sous-domaine public : <slug>.autoecoleconnect.fr.
CREATE TABLE auto_ecoles (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom        VARCHAR(255) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    adresse    TEXT,
    active     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE auto_ecoles;
