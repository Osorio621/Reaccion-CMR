package com.reactivosdelvalle.crm_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OportunidadEtapaHistResponse {

    private Long id;
    private Long oportunidadId;
    private Long etapaAnteriorId;
    private Long etapaNuevaId;
    private Long usuarioId;
    private String notas;
    private LocalDateTime createdAt;
}