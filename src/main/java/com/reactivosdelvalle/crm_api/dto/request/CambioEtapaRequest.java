package com.reactivosdelvalle.crm_api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioEtapaRequest {

    @NotNull(message = "La etapa nueva es obligatoria")
    private Long etapaNuevaId;

    private String notas;
}