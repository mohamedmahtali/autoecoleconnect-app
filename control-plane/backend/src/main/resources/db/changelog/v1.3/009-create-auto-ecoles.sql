--liquibase formatted sql

--changeset control-plane:009-create-auto-ecoles
-- Miroir leger des agences (docs/17, docs/18 lot 4). Le control-plane doit
-- les connaitre pour deux raisons : garantir l'unicite globale des slugs, qui
-- sont des sous-domaines publics, et composer la liste tenant.autoEcoles du
-- values.yaml GitOps. Les donnees metier de l'agence vivent, elles, dans la
-- base du tenant.
CREATE TABLE auto_ecoles (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID         NOT NULL REFERENCES tenants (id),
    nom        VARCHAR(255) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auto_ecoles_tenant ON auto_ecoles (tenant_id);

-- Chaque tenant existant devient une organisation a une agence, reprenant son
-- propre slug : avant la refonte, le tenant EST l'agence.
INSERT INTO auto_ecoles (tenant_id, nom, slug)
SELECT id, nom, slug FROM tenants WHERE deleted_at IS NULL;
--rollback DROP TABLE auto_ecoles;
