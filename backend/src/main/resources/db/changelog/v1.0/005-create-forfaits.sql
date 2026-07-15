--liquibase formatted sql

--changeset autoeecoleconnect:005-create-forfaits
CREATE TABLE forfaits (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom          VARCHAR(255)  NOT NULL,
    nombre_heure INTEGER       NOT NULL,
    validite     INTEGER       NOT NULL,
    unite        VARCHAR(10)   NOT NULL,             -- MOIS | JOUR
    prix         DECIMAL(10,2) NOT NULL,
    conditions   TEXT,
    categorie    VARCHAR(50)   NOT NULL,             -- CONDUITE | JOURNALIER
    transmission VARCHAR(20),                        -- MANUELLE | AUTOMATIQUE
    kilometrage  VARCHAR(20)   NOT NULL,             -- ILLIMITE | LIMITE
    nb_kilometre INTEGER,
    carburant    VARCHAR(20)   NOT NULL,             -- INCLUS | NON_INCLUS
    active       BOOLEAN       NOT NULL DEFAULT true
);
--rollback DROP TABLE forfaits;
