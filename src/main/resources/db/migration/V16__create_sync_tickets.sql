-- V16: Seguimiento de sincronizacion de clientes con el sistema de tickets (Next.js)
-- Un registro por cliente: estado del ultimo envio, ID externo y reintentos.

CREATE TABLE sync_tickets (
    id                   BIGSERIAL PRIMARY KEY,
    cliente_id           BIGINT NOT NULL UNIQUE REFERENCES clientes(id) ON DELETE CASCADE,
    ticket_cliente_id    BIGINT,
    estado               VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos             INT NOT NULL DEFAULT 0,
    ultimo_error         TEXT,
    fecha_ultimo_intento TIMESTAMP,
    enviado_en           TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_tickets_estado ON sync_tickets(estado);
