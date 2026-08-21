package com.reactivosdelvalle.crm_api.dto.response;

import com.reactivosdelvalle.crm_api.entity.EstadoOportunidad;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class OportunidadResponse {

    private Long id;
    private String nombre;
    private Long clienteId;
    private Long prospectoId;
    private Long ejecutivoId;
    private Long etapaId;
    private BigDecimal valor;
    private Integer probabilidad;
    private BigDecimal valorPonderado;
    private LocalDate fechaEstimadaCierre;
    private String proximaAccion;
    private LocalDate fechaProximaAccion;
    private String descripcion;
    private String competencia;
    private EstadoOportunidad estado;
    private String motivoPerdida;
    private LocalDate fechaCierreReal;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdById;
    private Long updatedById;
}