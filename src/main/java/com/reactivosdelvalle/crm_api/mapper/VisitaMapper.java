package com.reactivosdelvalle.crm_api.mapper;

import com.reactivosdelvalle.crm_api.dto.response.VisitaResponse;
import com.reactivosdelvalle.crm_api.entity.Visita;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Map;

/**
 * Los mapas de nombres se resuelven por lotes en el servicio
 * para evitar el problema N+1.
 */
@Mapper(componentModel = "spring")
public interface VisitaMapper {

    @Mapping(target = "id", source = "visita.id")
    @Mapping(target = "tipoEntidad", source = "visita.tipoEntidad")
    @Mapping(target = "clienteId", source = "visita.clienteId")
    @Mapping(target = "clienteNombre", expression = "java(clientesNombres.get(visita.getClienteId()))")
    @Mapping(target = "prospectoId", source = "visita.prospectoId")
    @Mapping(target = "prospectoNombre", expression = "java(prospectosNombres.get(visita.getProspectoId()))")
    @Mapping(target = "oportunidadId", source = "visita.oportunidadId")
    @Mapping(target = "oportunidadNombre", expression = "java(oportunidadesNombres.get(visita.getOportunidadId()))")
    @Mapping(target = "ejecutivoId", source = "visita.ejecutivoId")
    @Mapping(target = "ejecutivoNombre", expression = "java(ejecutivosNombres.get(visita.getEjecutivoId()))")
    @Mapping(target = "fecha", source = "visita.fecha")
    @Mapping(target = "objetivo", source = "visita.objetivo")
    @Mapping(target = "necesidadDetectada", source = "visita.necesidadDetectada")
    @Mapping(target = "competenciaMencionada", source = "visita.competenciaMencionada")
    @Mapping(target = "resultadoId", source = "visita.resultadoId")
    @Mapping(target = "resultadoNombre", expression = "java(resultadosNombres.get(visita.getResultadoId()))")
    @Mapping(target = "oportunidadGenerada", source = "visita.oportunidadGenerada")
    @Mapping(target = "compromiso", source = "visita.compromiso")
    @Mapping(target = "notasAdicionales", source = "visita.notasAdicionales")
    @Mapping(target = "createdAt", source = "visita.createdAt")
    @Mapping(target = "updatedAt", source = "visita.updatedAt")
    VisitaResponse toResponse(
            Visita visita,
            Map<Long, String> clientesNombres,
            Map<Long, String> prospectosNombres,
            Map<Long, String> oportunidadesNombres,
            Map<Long, String> ejecutivosNombres,
            Map<Long, String> resultadosNombres);
}
