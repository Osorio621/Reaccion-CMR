package com.reactivosdelvalle.crm_api.dto.response;

public record ClienteExportRow(
        String nombre,
        String razonSocial,
        String email,
        String telefono,
        String ejecutivo,
        String industria,
        String zona
) {}
