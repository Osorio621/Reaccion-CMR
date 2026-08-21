package com.reactivosdelvalle.crm_api.mapper;

import com.reactivosdelvalle.crm_api.dto.response.OportunidadResponse;
import com.reactivosdelvalle.crm_api.entity.Oportunidad;
import org.mapstruct.Mapper;

/**
 * valor_ponderado es una columna generada por la base de datos
 * (GENERATED ALWAYS AS valor * probabilidad / 100.0) y se mapea directamente.
 */
@Mapper(componentModel = "spring")
public interface OportunidadMapper {

    OportunidadResponse toResponse(Oportunidad oportunidad);
}
