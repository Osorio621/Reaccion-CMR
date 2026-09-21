package com.reactivosdelvalle.crm_api.dto.response;

public record ProspectoExportRow(
        String nombre,
        String empresa,
        String email,
        String etapa,
        String origen,
        String fechaCreacion
) {}
