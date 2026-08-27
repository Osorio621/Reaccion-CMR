package com.reactivosdelvalle.crm_api.scheduler;

import com.reactivosdelvalle.crm_api.service.TicketsIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintento periodico de clientes que no pudieron enviarse al sistema
 * de tickets (red caida, Next.js detenido, etc.). No hace nada si la
 * integracion esta deshabilitada.
 */
@Component
public class TicketSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketSyncScheduler.class);

    private final TicketsIntegrationService ticketsIntegrationService;

    public TicketSyncScheduler(TicketsIntegrationService ticketsIntegrationService) {
        this.ticketsIntegrationService = ticketsIntegrationService;
    }

    @Scheduled(cron = "${app.tickets.reintento-cron:0 */5 * * * *}")
    public void reintentarEnviosPendientes() {
        try {
            ticketsIntegrationService.reintentarPendientes();
        } catch (Exception e) {
            log.error("Error inesperado en reintento de tickets: {}", e.getMessage());
        }
    }
}
