package com.reactivosdelvalle.crm_api.dto.request;

import com.reactivosdelvalle.crm_api.entity.EstadoOportunidad;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CerrarOportunidadRequest {

    @NotNull(message = "El estado de cierre es obligatorio (GANADA, PERDIDA o CONGELADA)")
    private EstadoOportunidad estado;

    private String motivoPerdida;

    private LocalDate fechaCierreReal;
}