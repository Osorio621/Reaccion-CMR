package com.reactivosdelvalle.crm_api.scheduler;

import com.reactivosdelvalle.crm_api.entity.EstadoSeguimiento;
import com.reactivosdelvalle.crm_api.entity.Seguimiento;
import com.reactivosdelvalle.crm_api.repository.SeguimientoRepository;
import com.reactivosdelvalle.crm_api.service.NotificacionSeguimientoService;
import com.reactivosdelvalle.crm_api.service.SeguimientoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Regla de negocio: un seguimiento PENDIENTE cuya fecha_programada ya pasó
 * debe transicionar automáticamente a VENCIDO y notificar al ejecutivo
 * por email y WhatsApp.
 *
 * Corre cada mañana (configurable via app.vencimiento.cron).
 */
@Component
public class VencimientoScheduler {

    private static final Logger log = LoggerFactory.getLogger(VencimientoScheduler.class);

    private final SeguimientoService seguimientoService;
    private final SeguimientoRepository seguimientoRepository;
    private final NotificacionSeguimientoService notificacionService;

    public VencimientoScheduler(SeguimientoService seguimientoService,
                                 SeguimientoRepository seguimientoRepository,
                                 NotificacionSeguimientoService notificacionService) {
        this.seguimientoService = seguimientoService;
        this.seguimientoRepository = seguimientoRepository;
        this.notificacionService = notificacionService;
    }

    @Scheduled(cron = "${app.vencimiento.cron:0 0 6 * * *}")
    public void marcarSeguimientosVencidos() {
        // Capturar la lista ANTES de marcar, para tener los datos de notificación
        List<Seguimiento> aVencer = seguimientoRepository
                .findByEstadoAndFechaProgramadaBefore(EstadoSeguimiento.PENDIENTE, LocalDate.now());

        int actualizados = seguimientoService.marcarVencidos();

        if (actualizados > 0) {
            log.info("=== VENCIMIENTOS: {} seguimiento(s) PENDIENTE marcado(s) como VENCIDO ===", actualizados);
            notificacionService.notificarVencidos(aVencer);
        } else {
            log.info("=== VENCIMIENTOS: sin seguimientos nuevos por vencer hoy ===");
        }
    }
}
