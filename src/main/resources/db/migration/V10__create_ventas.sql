-- V10: Ventas (Regla 9: una fila por ejecutivo/año/mes)

CREATE TABLE ventas (
    id            BIGSERIAL PRIMARY KEY,
    ejecutivo_id  BIGINT NOT NULL REFERENCES usuarios (id),
    anio          INTEGER NOT NULL,
    mes           INTEGER NOT NULL,
    meta          NUMERIC(15,2) NOT NULL,
    venta_real    NUMERIC(15,2) DEFAULT 0,
    forecast      NUMERIC(15,2) DEFAULT 0,
    notas         TEXT,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW(),
    updated_by_id BIGINT REFERENCES usuarios (id),
    CONSTRAINT uk_ventas_ejecutivo_anio_mes UNIQUE (ejecutivo_id, anio, mes),
    CONSTRAINT ventas_anio_check     CHECK (anio >= 2020 AND anio <= 2100),
    CONSTRAINT ventas_mes_check      CHECK (mes >= 1 AND mes <= 12),
    CONSTRAINT ventas_meta_check     CHECK (meta >= 0),
    CONSTRAINT ventas_venta_real_check CHECK (venta_real >= 0),
    CONSTRAINT ventas_forecast_check CHECK (forecast >= 0)
);