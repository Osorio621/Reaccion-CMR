package com.reactivosdelvalle.crm_api.mapper;

import com.reactivosdelvalle.crm_api.dto.response.CatalogoResponse;
import com.reactivosdelvalle.crm_api.entity.Catalogo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CatalogoMapper {

    CatalogoResponse toResponse(Catalogo catalogo);
}
