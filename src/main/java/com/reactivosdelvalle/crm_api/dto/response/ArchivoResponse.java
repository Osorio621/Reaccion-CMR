package com.reactivosdelvalle.crm_api.dto.response;

import java.time.LocalDateTime;

public record ArchivoResponse(
        Long id,
        String entidadTipo,
        Long entidadId,
        String nombreOriginal,
        String tipoMime,
        Long tamanoBytes,
        String descripcion,
        Long subidoPorId,
        LocalDateTime createdAt
) {}
