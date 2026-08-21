package com.reactivosdelvalle.crm_api.mapper;

import com.reactivosdelvalle.crm_api.dto.response.ProspectoResponse;
import com.reactivosdelvalle.crm_api.entity.Prospecto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProspectoMapper {

    ProspectoResponse toResponse(Prospecto prospecto);
}
