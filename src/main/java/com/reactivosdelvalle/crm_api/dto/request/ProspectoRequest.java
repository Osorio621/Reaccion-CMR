package com.reactivosdelvalle.crm_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProspectoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    private String nombre;

    @Size(max = 200, message = "La empresa no puede superar 200 caracteres")
    private String empresa;

    private Long tipoId;

    private Long industriaId;

    private Long zonaId;

    private Long responsableId;

    @NotNull(message = "La etapa es obligatoria (Regla 5)")
    private Long etapaId;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    private String email;

    @Size(max = 300, message = "El sitio web no puede superar 300 caracteres")
    private String sitioWeb;

    private String notas;

    @NotBlank(message = "La próxima acción es obligatoria (Regla 5)")
    private String proximaAccion;

    @NotNull(message = "La fecha de la próxima acción es obligatoria (Regla 5)")
    private LocalDate fechaProximaAccion;
}