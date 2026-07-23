--liquibase formatted sql

--changeset autoeecoleconnect:014-create-examens
-- Table neuve (backlog #34) : contrairement aux colonnes auto_ecole_id
-- ajoutees sur des tables deja peuplees (v1.1), examens nait vide, donc
-- auto_ecole_id peut etre NOT NULL avec sa cle etrangere des le depart.
CREATE TABLE examens (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id        UUID        NOT NULL REFERENCES clients(id),
    auto_ecole_id    UUID        NOT NULL REFERENCES auto_ecoles(id),
    type             VARCHAR(50) NOT NULL,
                     -- CODE | CONDUITE
    date_examen      DATE        NOT NULL,
    date_convocation DATE,
    resultat         VARCHAR(50) NOT NULL DEFAULT 'PLANIFIE',
                     -- PLANIFIE | REUSSI | ECHOUE | ABSENT
    nombre_fautes    INTEGER,
    centre_examen    VARCHAR(255),
    examinateur      VARCHAR(255),
    notes            TEXT,
    active           BOOLEAN     NOT NULL DEFAULT true,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_examens_client ON examens (client_id);
CREATE INDEX idx_examens_auto_ecole ON examens (auto_ecole_id);
--rollback DROP TABLE examens;
