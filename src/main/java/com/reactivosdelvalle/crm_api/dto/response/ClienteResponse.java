package com.reactivosdelvalle.crm_api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ClienteResponse {

    private Long id;
    private String nombre;
    private String razonSocial;
    private String rfc;
    private Long tipoId;
    private Long industriaId;
    private Long zonaId;
    private Long ejecutivoId;
    private String telefonoPrincipal;
    private String emailPrincipal;
    private String sitioWeb;
    private String direccion;
    private String ciudad;
    private String estadoRegion;
    private String notas;
    private LocalDate fechaPrimeraCompra;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdById;
    private Long updatedById;
}