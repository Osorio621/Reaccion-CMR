package com.reactivosdelvalle.crm_api.service;

import java.math.BigDecimal;

public record PipelineExportRow(
        String etapa,
        long cantidad,
        BigDecimal valorTotal,
        BigDecimal valorPonderado
) {}