package com.reactivosdelvalle.crm_api.mapper;

import com.reactivosdelvalle.crm_api.dto.response.ContactoResponse;
import com.reactivosdelvalle.crm_api.entity.Contacto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContactoMapper {

    ContactoResponse toResponse(Contacto contacto);
}
