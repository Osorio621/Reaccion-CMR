-- V2: Tabla maestra de catálogos (listas desplegables polimórficas)

CREATE TABLE catalogos (
    id                  BIGSERIAL PRIMARY KEY,
    tipo                VARCHAR(50)  NOT NULL,
    codigo              VARCHAR(30)  NOT NULL,
    nombre              VARCHAR(100) NOT NULL,
    descripcion         TEXT,
    probabilidad_default INTEGER,
    orden               INTEGER      DEFAULT 0,
    activo              BOOLEAN      DEFAULT TRUE,
    created_at          TIMESTAMP    DEFAULT NOW(),
    updated_at          TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT catalogos_codigo_key             UNIQUE (codigo),
    CONSTRAINT catalogos_probabilidad_default_check CHECK (probabilidad_default >= 0 AND probabilidad_default <= 100)
);

CREATE INDEX idx_catalogos_tipo ON catalogos (tipo, activo);