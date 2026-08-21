-- V1: Tipos enumerados del CRM (PostgreSQL ENUMs)
-- Nota: los nombres de tipo coinciden con los esperados por Hibernate (@JdbcTypeCode NAMED_ENUM)

CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'GERENTE', 'EJECUTIVO');

CREATE TYPE estado_oportunidad AS ENUM ('ACTIVA', 'GANADA', 'PERDIDA', 'CONGELADA');

CREATE TYPE tipo_entidad_visita AS ENUM ('CLIENTE', 'PROSPECTO');

CREATE TYPE estado_seguimiento AS ENUM ('PENDIENTE', 'COMPLETADO', 'CANCELADO', 'VENCIDO');

CREATE TYPE accion_auditoria AS ENUM ('INSERT', 'UPDATE', 'DELETE_LOGICO');