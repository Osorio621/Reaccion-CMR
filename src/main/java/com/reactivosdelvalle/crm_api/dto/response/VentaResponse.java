package com.reactivosdelvalle.crm_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaResponse {
    private Long id;
    private Long ejecutivoId;
    private String ejecutivoNombre;
    private Integer anio;
    private Integer mes;
    private BigDecimal meta;
    private BigDecimal ventaReal;
    private BigDecimal forecast;
    private BigDecimal porcentajeCumplimiento;
    private String notas;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long updatedById;
    private String updatedByNombre;
}
