-- Fase 0-B: preferencias de notificación por canal en la tabla usuarios.
-- notif_email    → TRUE por defecto (notificaciones por correo habilitadas)
-- notif_whatsapp → FALSE por defecto (requiere número configurado para activar)
-- telefono_whatsapp → número exclusivo para WhatsApp (puede diferir de telefono)

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS notif_email       BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notif_whatsapp    BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS telefono_whatsapp VARCHAR(20);
