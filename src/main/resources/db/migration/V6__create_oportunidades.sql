-- V6: Oportunidades (Regla de Oro + valor ponderado generado)

CREATE TABLE oportunidades (
    id                    BIGSERIAL PRIMARY KEY,
    nombre                VARCHAR(200) NOT NULL,
    cliente_id            BIGINT NOT NULL REFERENCES clientes (id),
    prospecto_id          BIGINT REFERENCES prospectos (id),
    ejecutivo_id          BIGINT NOT NULL REFERENCES usuarios (id),
    etapa_id              BIGINT NOT NULL REFERENCES catalogos (id),
    valor                 NUMERIC(15,2) NOT NULL,
    probabilidad          INTEGER NOT NULL,
    valor_ponderado       NUMERIC(15,2) GENERATED ALWAYS AS ((valor * (probabilidad)::numeric) / 100.0) STORED,
    fecha_estimada_cierre DATE NOT NULL,
    proxima_accion        TEXT NOT NULL,
    fecha_proxima_accion  DATE NOT NULL,
    descripcion           TEXT,
    competencia           VARCHAR(300),
    estado                estado_oportunidad DEFAULT 'ACTIVA',
    motivo_perdida        TEXT,
    fecha_cierre_real     DATE,
    activo                BOOLEAN DEFAULT TRUE,
    created_at            TIMESTAMP DEFAULT NOW(),
    updated_at            TIMESTAMP DEFAULT NOW(),
    created_by_id         BIGINT REFERENCES usuarios (id),
    updated_by_id         BIGINT REFERENCES usuarios (id),
    CONSTRAINT oportunidades_valor_check        CHECK (valor > 0),
    CONSTRAINT oportunidades_probabilidad_check CHECK (probabilidad >= 0 AND probabilidad <= 100)
);

CREATE INDEX idx_oportunidades_ejecutivo ON oportunidades (ejecutivo_id, estado);
CREATE INDEX idx_oportunidades_etapa     ON oportunidades (etapa_id, activo);
CREATE INDEX idx_oportunidades_cierre    ON oportunidades (fecha_estimada_cierre) WHERE estado = 'ACTIVA';