--liquibase formatted sql

--changeset control-plane:002-create-tenants
CREATE TABLE tenants (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id       UUID         NOT NULL REFERENCES organisations(id),
    slug         VARCHAR(100) NOT NULL UNIQUE,
    namespace    VARCHAR(100) NOT NULL UNIQUE,
    nom          VARCHAR(255) NOT NULL,
    url          VARCHAR(255) NOT NULL,
    statut       VARCHAR(50)  NOT NULL DEFAULT 'provisioning',
                 -- provisioning | trial | active | suspended | deleted | failed
                 -- (failed : ajout Slice A, absent des docs/06 — provisioning
                 -- bloqué après échec GitHub ou timeout de sync ArgoCD)
    plan         VARCHAR(50)  NOT NULL DEFAULT 'solo',
    config       JSONB        NOT NULL DEFAULT '{}',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    suspended_at TIMESTAMP,
    deleted_at   TIMESTAMP
);

CREATE INDEX idx_tenants_org ON tenants (org_id);
CREATE INDEX idx_tenants_statut ON tenants (statut);
--rollback DROP TABLE tenants;
