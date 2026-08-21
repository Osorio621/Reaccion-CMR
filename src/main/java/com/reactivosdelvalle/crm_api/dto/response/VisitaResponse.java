package com.reactivosdelvalle.crm_api.dto.response;

import com.reactivosdelvalle.crm_api.entity.TipoEntidadVisita;
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
public class VisitaResponse {
    private Long id;
    private TipoEntidadVisita tipoEntidad;
    private Long clienteId;
    private String clienteNombre;
    private Long prospectoId;
    private String prospectoNombre;
    private Long oportunidadId;
    private String oportunidadNombre;
    private Long ejecutivoId;
    private String ejecutivoNombre;
    private LocalDate fecha;
    private String objetivo;
    private String necesidadDetectada;
    private String competenciaMencionada;
    private Long resultadoId;
    private String resultadoNombre;
    private Boolean oportunidadGenerada;
    private String compromiso;
    private String notasAdicionales;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
