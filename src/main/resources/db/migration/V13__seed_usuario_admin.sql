-- Seed del usuario administrador inicial (idempotente)
-- Contraseña: Admin123! (hash BCrypt)
INSERT INTO public.usuarios (nombre, apellido, email, password_hash, rol, activo, created_at, updated_at)
VALUES (
    'Administrador',
    'Sistema',
    'admin@reactivosdelvalle.com',
    '$2a$10$wmX9tTqb/KSnudLRzDOcl.QYMsvQoKx0P4ZgEou0TogiE3DdQo4S.',
    'ADMIN',
    true,
    now(),
    now()
)
ON CONFLICT (email) DO NOTHING;