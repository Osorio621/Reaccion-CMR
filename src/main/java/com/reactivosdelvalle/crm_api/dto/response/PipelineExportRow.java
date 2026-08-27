package com.reactivosdelvalle.crm_api.dto.response;

import java.math.BigDecimal;

public record PipelineExportRow(
        String etapa,
        long cantidad,
        BigDecimal valorTotal,
        BigDecimal valorPonderado
) {}
