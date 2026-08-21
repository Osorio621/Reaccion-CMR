-- V4: Clientes y contactos

CREATE TABLE clientes (
    id                   BIGSERIAL PRIMARY KEY,
    nombre               VARCHAR(200) NOT NULL,
    razon_social         VARCHAR(200),
    rfc                  VARCHAR(20),
    tipo_id              BIGINT REFERENCES catalogos (id),
    industria_id         BIGINT REFERENCES catalogos (id),
    zona_id              BIGINT REFERENCES catalogos (id),
    ejecutivo_id         BIGINT NOT NULL REFERENCES usuarios (id),
    telefono_principal   VARCHAR(20),
    email_principal      VARCHAR(150),
    sitio_web            VARCHAR(300),
    direccion            VARCHAR(300),
    ciudad               VARCHAR(100),
    estado_region        VARCHAR(100),
    notas                TEXT,
    fecha_primera_compra DATE,
    activo               BOOLEAN DEFAULT TRUE,
    created_at           TIMESTAMP DEFAULT NOW(),
    updated_at           TIMESTAMP DEFAULT NOW(),
    created_by_id        BIGINT REFERENCES usuarios (id),
    updated_by_id        BIGINT REFERENCES usuarios (id)
);

CREATE INDEX idx_clientes_ejecutivo ON clientes (ejecutivo_id, activo);
CREATE INDEX idx_clientes_nombre    ON clientes (nombre);

CREATE TABLE contactos (
    id            BIGSERIAL PRIMARY KEY,
    cliente_id    BIGINT NOT NULL REFERENCES clientes (id),
    nombre        VARCHAR(200) NOT NULL,
    cargo         VARCHAR(100),
    telefono      VARCHAR(20),
    email         VARCHAR(150),
    es_principal  BOOLEAN DEFAULT FALSE,
    activo        BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW()
);

-- Solo un contacto principal por cliente (Regla: contacto principal único)
CREATE UNIQUE INDEX idx_contactos_principal_unico ON contactos (cliente_id) WHERE es_principal = TRUE;