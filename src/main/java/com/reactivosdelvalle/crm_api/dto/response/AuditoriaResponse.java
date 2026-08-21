package com.reactivosdelvalle.crm_api.dto.response;

import com.reactivosdelvalle.crm_api.entity.AccionAuditoria;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditoriaResponse {

    private Long id;
    private String tablaNombre;
    private Long registroId;
    private Long usuarioId;
    private String usuarioNombre;
    private AccionAuditoria accion;
    private String campoModificado;
    private String valorAnterior;
    private String valorNuevo;
    private String ipAddress;
    private LocalDateTime createdAt;
}
