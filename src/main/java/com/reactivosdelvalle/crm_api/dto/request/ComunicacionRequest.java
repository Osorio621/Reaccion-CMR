package com.reactivosdelvalle.crm_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ComunicacionRequest(

        @NotBlank(message = "El tipo de comunicacion es obligatorio")
        String tipo,

        @NotNull(message = "La fecha es obligatoria")
        LocalDateTime fecha,

        String descripcion,

        String resultado
) {}
