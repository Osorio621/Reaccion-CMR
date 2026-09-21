-- V15: Tokens de un solo uso para restablecimiento de contrasena
-- Se guarda el SHA-256 del token, nunca el token en claro.

CREATE TABLE tokens_reseteo (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_en TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tokens_reseteo_usuario ON tokens_reseteo(usuario_id);
