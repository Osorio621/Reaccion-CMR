package com.reactivosdelvalle.crm_api.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OportunidadExportRow(
        String nombre,
        String cliente,
        String etapa,
        BigDecimal valor,
        Integer probabilidad,
        String estado,
        LocalDate fechaCierre
) {}