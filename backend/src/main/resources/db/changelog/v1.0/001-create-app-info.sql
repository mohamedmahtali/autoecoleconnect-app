--liquibase formatted sql

--changeset autoeecoleconnect:001-create-app-info
CREATE TABLE app_info (
    id      BIGSERIAL PRIMARY KEY,
    cle     VARCHAR(100) NOT NULL UNIQUE,
    valeur  VARCHAR(255) NOT NULL,
    cree_le TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO app_info (cle, valeur) VALUES ('schema.version', 'v1.0-phase0');
--rollback DROP TABLE app_info;
