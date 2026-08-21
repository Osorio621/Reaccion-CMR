-- V11: Auditoría (append-only; se inserta desde el AuditListener JPA - Parte 3)

CREATE TABLE auditoria (
    id                BIGSERIAL PRIMARY KEY,
    tabla_nombre      VARCHAR(60) NOT NULL,
    registro_id       BIGINT NOT NULL,
    usuario_id        BIGINT REFERENCES usuarios (id),
    accion            accion_auditoria NOT NULL,
    campo_modificado  VARCHAR(100),
    valor_anterior    TEXT,
    valor_nuevo       TEXT,
    ip_address        VARCHAR(45),
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_auditoria_tabla_registro ON auditoria (tabla_nombre, registro_id);
CREATE INDEX idx_auditoria_usuario        ON auditoria (usuario_id, created_at DESC);