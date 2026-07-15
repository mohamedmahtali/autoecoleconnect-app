--liquibase formatted sql

--changeset autoeecoleconnect:003-align-clients-schema-cible
-- Aligne la table clients sur le schéma cible (docs/06-modele-donnees.md §6.3) :
-- id UUID, password_hash, adresse, notes, soft delete via active.
-- Drop/recreate assumé : projet pré-production, aucune donnée à préserver
-- (la règle « ne jamais modifier un changeset appliqué » impose ce nouveau fichier).
DROP TABLE clients;

CREATE TABLE clients (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom           VARCHAR(100)  NOT NULL,
    prenom        VARCHAR(100)  NOT NULL,
    email         VARCHAR(255)  NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    telephone     VARCHAR(20),
    adresse       TEXT,
    notes         TEXT,
    active        BOOLEAN       NOT NULL DEFAULT true,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE clients;
