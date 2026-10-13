-- S1: Fuerza el cambio de contraseña en el primer inicio de sesión.
-- La cuenta admin del seed siempre se entrega con este flag activo
-- para que la instalación productiva no opere con la clave pública del repo.
ALTER TABLE public.usuarios
    ADD COLUMN IF NOT EXISTS requiere_cambio_password BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE public.usuarios
SET requiere_cambio_password = TRUE
WHERE email = 'admin@reactivosdelvalle.com';
