package com.reactivosdelvalle.crm_api.service;

public record ProspectoExportRow(
        String nombre,
        String empresa,
        String email,
        String etapa,
        String origen,
        String fechaCreacion
) {}