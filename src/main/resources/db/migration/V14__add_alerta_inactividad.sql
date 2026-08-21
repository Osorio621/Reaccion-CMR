-- V14: Marca de tiempo de la última alerta de inactividad enviada a cada usuario
-- La escribe el InactividadScheduler (Parte 4) para trazabilidad de alertas diarias.

ALTER TABLE usuarios ADD COLUMN alerta_inactividad TIMESTAMP;
