package com.reactivosdelvalle.crm_api.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OportunidadRequest {

    @NotBlank(message = "El nombre es obligatorio (Regla de Oro)")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    private String nombre;

    @NotNull(message = "El cliente es obligatorio (Regla de Oro)")
    private Long clienteId;

    private Long prospectoId;

    private Long ejecutivoId;

    @NotNull(message = "La etapa es obligatoria (Regla de Oro)")
    private Long etapaId;

    @NotNull(message = "El valor es obligatorio (Regla de Oro)")
    @DecimalMin(value = "0.01", message = "El valor debe ser mayor a cero")
    private BigDecimal valor;

    @NotNull(message = "La probabilidad es obligatoria (Regla de Oro)")
    @Min(value = 0, message = "La probabilidad no puede ser menor a 0")
    @Max(value = 100, message = "La probabilidad no puede ser mayor a 100")
    private Integer probabilidad;

    @NotNull(message = "La fecha estimada de cierre es obligatoria (Regla de Oro)")
    private LocalDate fechaEstimadaCierre;

    @NotBlank(message = "La próxima acción es obligatoria (Regla de Oro)")
    private String proximaAccion;

    @NotNull(message = "La fecha de la próxima acción es obligatoria")
    private LocalDate fechaProximaAccion;

    private String descripcion;

    @Size(max = 300, message = "La competencia no puede superar 300 caracteres")
    private String competencia;
}