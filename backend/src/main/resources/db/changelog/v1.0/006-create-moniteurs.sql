--liquibase formatted sql

--changeset autoeecoleconnect:006-create-moniteurs
CREATE TABLE moniteurs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom           VARCHAR(100) NOT NULL,
    prenom        VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    telephone     VARCHAR(20),
    statut        VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
                  -- PENDING | APPROVED | REJECTED | INACTIVE
    notes         TEXT,
    active        BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE moniteurs;
