package com.reactivosdelvalle.crm_api.dto.response;

import com.reactivosdelvalle.crm_api.entity.EstadoSeguimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeguimientoResponse {
    private Long id;
    private Long oportunidadId;
    private String oportunidadNombre;
    private String clienteNombre;
    private Long ejecutivoId;
    private String ejecutivoNombre;
    private String tipo;
    private LocalDate fechaProgramada;
    private LocalDate fechaRealizada;
    private EstadoSeguimiento estado;
    private String notas;
    private String proximaAccion;
    private Long diasVencidos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
