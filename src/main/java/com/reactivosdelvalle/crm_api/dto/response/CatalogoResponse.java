package com.reactivosdelvalle.crm_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CatalogoResponse {

    private Long id;
    private String tipo;
    private String codigo;
    private String nombre;
    private String descripcion;
    private Integer probabilidadDefault;
    private Integer orden;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}