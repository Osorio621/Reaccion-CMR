-- Ruta de almacenamiento local de la foto de perfil (separada de foto_url que es la URL publica)
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS foto_ruta VARCHAR(500);
