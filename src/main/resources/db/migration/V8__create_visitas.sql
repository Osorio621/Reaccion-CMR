-- V8: Visitas (Regla 2: fecha máx. 24 h atrás · Regla 6: 7 campos obligatorios)

CREATE TABLE visitas (
    id                     BIGSERIAL PRIMARY KEY,
    tipo_entidad           tipo_entidad_visita NOT NULL,
    cliente_id             BIGINT REFERENCES clientes (id),
    prospecto_id           BIGINT REFERENCES prospectos (id),
    oportunidad_id         BIGINT REFERENCES oportunidades (id),
    ejecutivo_id           BIGINT NOT NULL REFERENCES usuarios (id),
    fecha                  DATE NOT NULL,
    objetivo               TEXT NOT NULL,
    necesidad_detectada    TEXT NOT NULL,
    competencia_mencionada VARCHAR(300) NOT NULL,
    resultado_id           BIGINT NOT NULL REFERENCES catalogos (id),
    oportunidad_generada   BOOLEAN NOT NULL,
    compromiso             TEXT NOT NULL,
    notas_adicionales      TEXT,
    created_at             TIMESTAMP DEFAULT NOW(),
    updated_at             TIMESTAMP DEFAULT NOW(),
    CONSTRAINT chk_visita_entidad CHECK (
        (tipo_entidad = 'CLIENTE'   AND cliente_id   IS NOT NULL AND prospecto_id IS NULL) OR
        (tipo_entidad = 'PROSPECTO' AND prospecto_id IS NOT NULL AND cliente_id   IS NULL)
    ),
    CONSTRAINT chk_visita_fecha CHECK (fecha >= CURRENT_DATE - INTERVAL '1 day')
);

CREATE INDEX idx_visitas_ejecutivo_fecha ON visitas (ejecutivo_id, fecha DESC);