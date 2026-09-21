package com.reactivosdelvalle.crm_api.dto.response;

import java.time.LocalDateTime;

public record ComunicacionResponse(
        Long id,
        String entidadTipo,
        Long entidadId,
        String tipo,
        LocalDateTime fecha,
        String descripcion,
        String resultado,
        Long ejecutivoId,
        String ejecutivoNombre,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
