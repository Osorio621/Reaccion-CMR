package com.reactivosdelvalle.crm_api.mapper;

import com.reactivosdelvalle.crm_api.dto.response.OportunidadEtapaHistResponse;
import com.reactivosdelvalle.crm_api.entity.OportunidadEtapaHist;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OportunidadEtapaHistMapper {

    OportunidadEtapaHistResponse toResponse(OportunidadEtapaHist historial);
}
