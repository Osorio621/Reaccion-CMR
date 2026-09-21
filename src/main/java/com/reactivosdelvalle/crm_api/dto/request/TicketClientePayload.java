package com.reactivosdelvalle.crm_api.dto.request;

import java.time.LocalDateTime;

/**
 * Cuerpo que el CRM envia al sistema de tickets al sincronizar un cliente.
 * El sistema de tickets hace upsert usando crmClienteId como clave.
 */
public record TicketClientePayload(
        Long crmClienteId,
        String nombre,
        String razonSocial,
        String rfc,
        String email,
        String telefono,
        String direccion,
        String ciudad,
        String estadoRegion,
        String sitioWeb,
        String ejecutivoEmail,
        String origen,
        LocalDateTime fechaEnvio
) {
}
