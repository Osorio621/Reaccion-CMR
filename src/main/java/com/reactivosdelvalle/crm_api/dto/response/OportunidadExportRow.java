package com.reactivosdelvalle.crm_api.dto.response;

import java.math.BigDecimal;

public record OportunidadExportRow(
        String nombre,
        String cliente,
        String etapa,
        BigDecimal valor,
        Integer probabilidad,
        String estado,
        String fechaCierre
) {}
