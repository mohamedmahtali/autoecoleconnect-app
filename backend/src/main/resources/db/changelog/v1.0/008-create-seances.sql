--liquibase formatted sql

--changeset autoeecoleconnect:008-create-seances
CREATE TABLE seances (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id     UUID        NOT NULL REFERENCES reservations(id),
    moniteur_id        UUID        REFERENCES moniteurs(id),
    voiture_id         UUID        REFERENCES voitures(id),
    date_seance        DATE        NOT NULL,
    h_deb              TIME        NOT NULL,
    h_fin              TIME        NOT NULL,
    statut             VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
                       -- SCHEDULED | COMPLETED | CANCELLED | NO_SHOW
    validated_client   BOOLEAN     NOT NULL DEFAULT false,
    validated_moniteur BOOLEAN     NOT NULL DEFAULT false,
    validated_admin    BOOLEAN     NOT NULL DEFAULT false,
    notes              TEXT,
    active             BOOLEAN     NOT NULL DEFAULT true,
    created_at         TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seances_reservation ON seances (reservation_id);
CREATE INDEX idx_seances_moniteur_date ON seances (moniteur_id, date_seance);
CREATE INDEX idx_seances_voiture_date ON seances (voiture_id, date_seance);
--rollback DROP TABLE seances;
