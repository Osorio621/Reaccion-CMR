-- V3: Usuarios del sistema (ADMIN, GERENTE, EJECUTIVO)

CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    apellido        VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    rol             rol_usuario  NOT NULL,
    telefono        VARCHAR(20),
    foto_url        VARCHAR(500),
    activo          BOOLEAN      DEFAULT TRUE,
    ultima_actividad TIMESTAMP,
    created_at      TIMESTAMP    DEFAULT NOW(),
    updated_at      TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT usuarios_email_key UNIQUE (email)
);