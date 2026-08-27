package com.reactivosdelvalle.crm_api.dto.response;

import java.time.LocalDateTime;

/** Estado de sincronizacion de un cliente con el sistema de tickets. */
public record SyncTicketResponse(
        Long clienteId,
        String estado,
        Long ticketClienteId,
        Integer intentos,
        String ultimoError,
        LocalDateTime fechaUltimoIntento,
        LocalDateTime enviadoEn
) {
}
