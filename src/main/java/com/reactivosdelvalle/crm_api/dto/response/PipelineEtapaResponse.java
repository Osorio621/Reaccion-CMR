package com.reactivosdelvalle.crm_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PipelineEtapaResponse {

    private Long etapaId;
    private String etapaNombre;
    private Long cantidad;
    private BigDecimal valorPonderadoTotal;
}