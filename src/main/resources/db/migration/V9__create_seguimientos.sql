-- V9: Seguimientos (dias_vencidos se calcula en la consulta, no se almacena)

CREATE TABLE seguimientos (
    id               BIGSERIAL PRIMARY KEY,
    oportunidad_id   BIGINT NOT NULL REFERENCES oportunidades (id),
    ejecutivo_id     BIGINT NOT NULL REFERENCES usuarios (id),
    tipo             VARCHAR(50) NOT NULL,
    fecha_programada DATE NOT NULL,
    fecha_realizada  DATE,
    estado           estado_seguimiento DEFAULT 'PENDIENTE',
    notas            TEXT,
    proxima_accion   TEXT,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_seguimientos_oportunidad ON seguimientos (oportunidad_id);
CREATE INDEX idx_seguimientos_vencidos    ON seguimientos (fecha_programada) WHERE estado = 'PENDIENTE';