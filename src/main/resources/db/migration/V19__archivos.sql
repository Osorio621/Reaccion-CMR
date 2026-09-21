-- Fase 4: adjuntos por entidad.
-- Soporta CLIENTE, PROSPECTO, OPORTUNIDAD, VISITA.
-- ruta_almacenada es relativa al directorio base (app.storage.ruta-local).

CREATE TYPE tipo_entidad_archivo AS ENUM ('CLIENTE', 'PROSPECTO', 'OPORTUNIDAD', 'VISITA');

CREATE TABLE archivos (
    id                BIGSERIAL            PRIMARY KEY,
    entidad_tipo      tipo_entidad_archivo NOT NULL,
    entidad_id        BIGINT               NOT NULL,
    nombre_original   VARCHAR(255)         NOT NULL,
    ruta_almacenada   VARCHAR(500)         NOT NULL,
    tipo_mime         VARCHAR(100)         NOT NULL,
    tamano_bytes      BIGINT               NOT NULL,
    descripcion       VARCHAR(300),
    subido_por_id     BIGINT               REFERENCES usuarios(id),
    created_at        TIMESTAMP            NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_archivos_entidad ON archivos(entidad_tipo, entidad_id);
