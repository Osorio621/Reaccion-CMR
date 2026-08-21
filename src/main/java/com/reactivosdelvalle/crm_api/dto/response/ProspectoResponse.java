package com.reactivosdelvalle.crm_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProspectoResponse {

    private Long id;
    private String nombre;
    private String empresa;
    private Long tipoId;
    private Long industriaId;
    private Long zonaId;
    private Long responsableId;
    private Long etapaId;
    private String telefono;
    private String email;
    private String sitioWeb;
    private String notas;
    private String proximaAccion;
    private LocalDate fechaProximaAccion;
    private Boolean convertido;
    private Long clienteId;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdById;
}