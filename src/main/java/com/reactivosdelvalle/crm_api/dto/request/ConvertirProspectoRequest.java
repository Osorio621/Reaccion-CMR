package com.reactivosdelvalle.crm_api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvertirProspectoRequest {

    @Size(max = 200, message = "La razón social no puede superar 200 caracteres")
    private String razonSocial;

    @Size(max = 20, message = "El RFC no puede superar 20 caracteres")
    private String rfc;

    private Long tipoId;

    private Long industriaId;

    private Long zonaId;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefonoPrincipal;

    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    private String emailPrincipal;

    @Size(max = 300, message = "El sitio web no puede superar 300 caracteres")
    private String sitioWeb;

    @Size(max = 300, message = "La dirección no puede superar 300 caracteres")
    private String direccion;

    @Size(max = 100, message = "La ciudad no puede superar 100 caracteres")
    private String ciudad;

    @Size(max = 100, message = "El estado/región no puede superar 100 caracteres")
    private String estadoRegion;

    private String notas;

    private LocalDate fechaPrimeraCompra;

    /**
     * OPCIONAL. Si se envía, además del cliente se crea la primera oportunidad
     * del negocio (Regla de Oro aplicada: nombre, etapa, valor, probabilidad,
     * fecha estimada de cierre y próxima acción son obligatorios aquí dentro).
     */
    @Valid
    private OportunidadInicial oportunidad;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OportunidadInicial {

        @Size(max = 200, message = "El nombre de la oportunidad no puede superar 200 caracteres")
        private String nombre;

        private Long etapaId;

        private BigDecimal valor;

        private Integer probabilidad;

        private LocalDate fechaEstimadaCierre;

        @Size(max = 300, message = "La próxima acción no puede superar 300 caracteres")
        private String proximaAccion;

        private LocalDate fechaProximaAccion;

        private String descripcion;

        private String competencia;
    }
}