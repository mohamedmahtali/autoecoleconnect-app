--liquibase formatted sql

--changeset control-plane:003-create-provisioning-logs
CREATE TABLE provisioning_logs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID        NOT NULL REFERENCES tenants(id),
    action     VARCHAR(100) NOT NULL,
               -- provision | suspend | resume | delete
    statut     VARCHAR(50) NOT NULL,
               -- pending | success | failed
    detail     TEXT,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_provisioning_logs_tenant ON provisioning_logs (tenant_id);
--rollback DROP TABLE provisioning_logs;
