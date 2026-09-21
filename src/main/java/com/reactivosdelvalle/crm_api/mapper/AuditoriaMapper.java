package com.reactivosdelvalle.crm_api.mapper;

import com.reactivosdelvalle.crm_api.dto.response.AuditoriaResponse;
import com.reactivosdelvalle.crm_api.entity.Auditoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

/**
 * El mapa de nombres de usuarios se resuelve por lotes en el servicio
 * para evitar el problema N+1.
 */
@Mapper(componentModel = "spring")
public interface AuditoriaMapper {

    @Mapping(target = "id", source = "auditoria.id")
    @Mapping(target = "tablaNombre", source = "auditoria.tablaNombre")
    @Mapping(target = "registroId", source = "auditoria.registroId")
    @Mapping(target = "usuarioId", source = "auditoria.usuarioId")
    @Mapping(target = "usuarioNombre", expression = "java(usuariosNombres.get(auditoria.getUsuarioId()))")
    @Mapping(target = "accion", source = "auditoria.accion")
    @Mapping(target = "campoModificado", source = "auditoria.campoModificado")
    @Mapping(target = "valorAnterior", source = "auditoria.valorAnterior")
    @Mapping(target = "valorNuevo", source = "auditoria.valorNuevo")
    @Mapping(target = "ipAddress", source = "auditoria.ipAddress")
    @Mapping(target = "descripcion", source = "auditoria.descripcion")
    @Mapping(target = "createdAt", source = "auditoria.createdAt")
    AuditoriaResponse toResponse(Auditoria auditoria, Map<Long, String> usuariosNombres);
}
