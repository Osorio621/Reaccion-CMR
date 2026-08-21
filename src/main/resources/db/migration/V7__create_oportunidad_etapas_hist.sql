-- V7: Historial de cambios de etapa de oportunidades (append-only)

CREATE TABLE oportunidad_etapas_hist (
    id                BIGSERIAL PRIMARY KEY,
    oportunidad_id    BIGINT NOT NULL REFERENCES oportunidades (id),
    etapa_anterior_id BIGINT REFERENCES catalogos (id),
    etapa_nueva_id    BIGINT NOT NULL REFERENCES catalogos (id),
    usuario_id        BIGINT NOT NULL REFERENCES usuarios (id),
    notas             TEXT,
    created_at        TIMESTAMP DEFAULT NOW()
);