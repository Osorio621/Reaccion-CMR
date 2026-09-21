-- Fase 5: Historial de comunicaciones
-- Tipos de comunicacion en el catalogo
INSERT INTO public.catalogos (tipo, codigo, nombre, descripcion, probabilidad_default, orden, activo, created_at, updated_at)
VALUES
    ('TIPO_COMUNICACION', 'COM_LLAMADA_SALIENTE', 'Llamada saliente', 'Llamada realizada al cliente/prospecto', NULL, 1, true, now(), now()),
    ('TIPO_COMUNICACION', 'COM_LLAMADA_ENTRANTE', 'Llamada entrante', 'Llamada recibida del cliente/prospecto', NULL, 2, true, now(), now()),
    ('TIPO_COMUNICACION', 'COM_EMAIL_ENVIADO',    'Email enviado',    'Correo enviado al cliente/prospecto',    NULL, 3, true, now(), now()),
    ('TIPO_COMUNICACION', 'COM_EMAIL_RECIBIDO',   'Email recibido',   'Correo recibido del cliente/prospecto',  NULL, 4, true, now(), now()),
    ('TIPO_COMUNICACION', 'COM_REUNION',          'Reunion',          'Reunion presencial o virtual',           NULL, 5, true, now(), now()),
    ('TIPO_COMUNICACION', 'COM_WHATSAPP',         'WhatsApp',         'Mensaje por WhatsApp',                   NULL, 6, true, now(), now()),
    ('TIPO_COMUNICACION', 'COM_VISITA',           'Visita',           'Visita presencial',                      NULL, 7, true, now(), now()),
    ('TIPO_COMUNICACION', 'COM_OTRO',             'Otro',             'Otro tipo de comunicacion',              NULL, 8, true, now(), now())
ON CONFLICT (codigo) DO NOTHING;

CREATE TYPE tipo_entidad_comunicacion AS ENUM ('CLIENTE', 'PROSPECTO');

CREATE TABLE comunicaciones (
    id              BIGSERIAL                   PRIMARY KEY,
    entidad_tipo    tipo_entidad_comunicacion   NOT NULL,
    entidad_id      BIGINT                      NOT NULL,
    tipo            VARCHAR(50)                 NOT NULL,
    fecha           TIMESTAMP                   NOT NULL,
    descripcion     TEXT,
    resultado       VARCHAR(300),
    ejecutivo_id    BIGINT REFERENCES usuarios(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comunicaciones_entidad   ON comunicaciones(entidad_tipo, entidad_id);
CREATE INDEX idx_comunicaciones_fecha     ON comunicaciones(fecha DESC);
CREATE INDEX idx_comunicaciones_ejecutivo ON comunicaciones(ejecutivo_id);
