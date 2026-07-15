--liquibase formatted sql

--changeset autoeecoleconnect:002-create-clients
CREATE TABLE clients (
    id             BIGSERIAL PRIMARY KEY,
    nom            VARCHAR(100)  NOT NULL,
    prenom         VARCHAR(100)  NOT NULL,
    email          VARCHAR(255)  NOT NULL UNIQUE,
    telephone      VARCHAR(20),
    date_naissance DATE,
    statut         VARCHAR(20)   NOT NULL DEFAULT 'PROSPECT',
    cree_le        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    modifie_le     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_clients_statut ON clients (statut);
--rollback DROP TABLE clients;
