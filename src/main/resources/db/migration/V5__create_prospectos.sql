-- V5: Prospectos (Regla 5: responsable y etapa son obligatorios)

CREATE TABLE prospectos (
    id                   BIGSERIAL PRIMARY KEY,
    nombre               VARCHAR(200) NOT NULL,
    empresa              VARCHAR(200),
    tipo_id              BIGINT REFERENCES catalogos (id),
    industria_id         BIGINT REFERENCES catalogos (id),
    zona_id              BIGINT REFERENCES catalogos (id),
    responsable_id       BIGINT NOT NULL REFERENCES usuarios (id),
    etapa_id             BIGINT NOT NULL REFERENCES catalogos (id),
    telefono             VARCHAR(20),
    email                VARCHAR(150),
    sitio_web            VARCHAR(300),
    notas                TEXT,
    proxima_accion       TEXT NOT NULL,
    fecha_proxima_accion DATE NOT NULL,
    convertido           BOOLEAN DEFAULT FALSE,
    cliente_id           BIGINT REFERENCES clientes (id),
    activo               BOOLEAN DEFAULT TRUE,
    created_at           TIMESTAMP DEFAULT NOW(),
    updated_at           TIMESTAMP DEFAULT NOW(),
    created_by_id        BIGINT REFERENCES usuarios (id)
);

CREATE INDEX idx_prospectos_responsable ON prospectos (responsable_id, activo);
CREATE INDEX idx_prospectos_etapa       ON prospectos (etapa_id);