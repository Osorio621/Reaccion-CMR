package com.reactivosdelvalle.crm_api.dto.request;

import com.reactivosdelvalle.crm_api.entity.EstadoSeguimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoRequest {

    @NotNull(message = "La oportunidad asociada es obligatoria")
    private Long oportunidadId;

    @NotBlank(message = "El tipo de actividad es obligatorio")
    private String tipo;

    @NotNull(message = "La fecha programada es obligatoria")
    private LocalDate fechaProgramada;

    private LocalDate fechaRealizada;

    private EstadoSeguimiento estado;

    private String notas;

    private String proximaAccion;
}
