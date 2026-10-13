-- Configuracion del logo del sistema (tabla de una sola fila)
CREATE TABLE config_logo (
    id               BIGINT      PRIMARY KEY DEFAULT 1,
    logo_url         VARCHAR(500),
    ruta_almacenada  VARCHAR(500),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_config_logo_single_row CHECK (id = 1)
);

-- Fila inicial sin logo configurado
INSERT INTO config_logo (id, logo_url, ruta_almacenada, updated_at)
VALUES (1, NULL, NULL, NOW());
