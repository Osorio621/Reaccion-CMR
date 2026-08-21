package com.reactivosdelvalle.crm_api.dto.request;

import com.reactivosdelvalle.crm_api.entity.TipoEntidadVisita;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitaRequest {

    @NotNull(message = "El tipo de entidad (CLIENTE o PROSPECTO) es obligatorio")
    private TipoEntidadVisita tipoEntidad;

    private Long clienteId;

    private Long prospectoId;

    private Long oportunidadId;

    @NotNull(message = "La fecha de la visita es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "El objetivo es obligatorio")
    private String objetivo;

    @NotBlank(message = "La necesidad detectada es obligatoria")
    private String necesidadDetectada;

    @NotBlank(message = "La competencia mencionada es obligatoria")
    private String competenciaMencionada;

    @NotNull(message = "El resultado es obligatorio")
    private Long resultadoId;

    @NotNull(message = "Debe indicar si se generó una oportunidad")
    private Boolean oportunidadGenerada;

    @NotBlank(message = "El compromiso es obligatorio")
    private String compromiso;

    private String notasAdicionales;
}
