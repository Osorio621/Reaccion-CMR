package com.reactivosdelvalle.crm_api.mapper;

import com.reactivosdelvalle.crm_api.dto.response.SeguimientoResponse;
import com.reactivosdelvalle.crm_api.entity.Seguimiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

/**
 * Los mapas de nombres se resuelven por lotes en el servicio
 * para evitar el problema N+1.
 * clienteNombresPorOportunidad: clave = oportunidad_id, valor = nombre del cliente de esa oportunidad.
 */
@Mapper(componentModel = "spring")
public interface SeguimientoMapper {

    @Mapping(target = "id", source = "seguimiento.id")
    @Mapping(target = "oportunidadId", source = "seguimiento.oportunidadId")
    @Mapping(target = "oportunidadNombre", expression = "java(oportunidadesNombres.get(seguimiento.getOportunidadId()))")
    @Mapping(target = "clienteNombre", expression = "java(clienteNombresPorOportunidad.get(seguimiento.getOportunidadId()))")
    @Mapping(target = "ejecutivoId", source = "seguimiento.ejecutivoId")
    @Mapping(target = "ejecutivoNombre", expression = "java(ejecutivosNombres.get(seguimiento.getEjecutivoId()))")
    @Mapping(target = "tipo", expression = "java(tiposNombres.getOrDefault(seguimiento.getTipo(), seguimiento.getTipo()))")
    @Mapping(target = "fechaProgramada", source = "seguimiento.fechaProgramada")
    @Mapping(target = "fechaRealizada", source = "seguimiento.fechaRealizada")
    @Mapping(target = "estado", source = "seguimiento.estado")
    @Mapping(target = "notas", source = "seguimiento.notas")
    @Mapping(target = "proximaAccion", source = "seguimiento.proximaAccion")
    @Mapping(target = "diasVencidos", source = "seguimiento.diasVencidos")
    @Mapping(target = "createdAt", source = "seguimiento.createdAt")
    @Mapping(target = "updatedAt", source = "seguimiento.updatedAt")
    SeguimientoResponse toResponse(
            Seguimiento seguimiento,
            Map<Long, String> oportunidadesNombres,
            Map<Long, String> clienteNombresPorOportunidad,
            Map<Long, String> ejecutivosNombres,
            Map<String, String> tiposNombres);
}
