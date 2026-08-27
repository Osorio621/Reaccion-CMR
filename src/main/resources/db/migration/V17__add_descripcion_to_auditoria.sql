-- V17: Agregar campo descripcion legible a la tabla de auditoria

ALTER TABLE auditoria ADD COLUMN descripcion TEXT;

-- Backfill: generar descripciones para registros existentes
UPDATE auditoria SET descripcion = CASE
    WHEN accion = 'INSERT' THEN 'Creó registro en ' || tabla_nombre || ' (id: ' || registro_id || ')'
    WHEN accion = 'DELETE_LOGICO' THEN 'Desactivó registro en ' || tabla_nombre || ' (id: ' || registro_id || ')'
    WHEN accion = 'UPDATE' AND campo_modificado IS NOT NULL THEN
        'Cambió ' || campo_modificado || ' de ''' || COALESCE(valor_anterior, 'vacío') || ''' a ''' || COALESCE(valor_nuevo, 'vacío') || ''''
    ELSE accion::text || ' en ' || tabla_nombre || ' (id: ' || registro_id || ')'
END WHERE descripcion IS NULL;
