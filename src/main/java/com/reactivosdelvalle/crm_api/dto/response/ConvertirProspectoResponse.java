package com.reactivosdelvalle.crm_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado de convertir un prospecto: siempre incluye el cliente creado y,
 * si el front lo solicitó, la oportunidad inicial generada en la misma
 * transacción.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertirProspectoResponse {

    private ClienteResponse cliente;

    private OportunidadResponse oportunidad;
}
