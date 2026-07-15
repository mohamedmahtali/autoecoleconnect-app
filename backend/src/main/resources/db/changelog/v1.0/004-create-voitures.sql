--liquibase formatted sql

--changeset autoeecoleconnect:004-create-voitures
CREATE TABLE voitures (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom             VARCHAR(100) NOT NULL,
    marque          VARCHAR(100) NOT NULL,
    transmission    VARCHAR(20)  NOT NULL,           -- MANUELLE | AUTOMATIQUE
    double_commande BOOLEAN      NOT NULL DEFAULT false,
    carburant       VARCHAR(50),
    couleur         VARCHAR(50),
    nb_portes       INTEGER,
    nb_passagers    INTEGER,
    air_conditionne BOOLEAN      NOT NULL DEFAULT false,
    note            TEXT,
    active          BOOLEAN      NOT NULL DEFAULT true
);
--rollback DROP TABLE voitures;
