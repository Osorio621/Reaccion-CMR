package com.reactivosdelvalle.crm_api.scheduler;

import com.reactivosdelvalle.crm_api.entity.Seguimiento;
import com.reactivosdelvalle.crm_api.repository.SeguimientoRepository;
import com.reactivosdelvalle.crm_api.service.NotificacionSeguimientoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Recordatorio preventivo: 1 día antes de la fecha del seguimiento notifica
 * al ejecutivo por email y WhatsApp para que llegue preparado.
 *
 * Corre a las 8 AM (configurable via app.recordatorio.cron).
 */
@Component
public class RecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioScheduler.class);

    private final SeguimientoRepository seguimientoRepository;
    private final NotificacionSeguimientoService notificacionService;

    public RecordatorioScheduler(SeguimientoRepository seguimientoRepository,
                                  NotificacionSeguimientoService notificacionService) {
        this.seguimientoRepository = seguimientoRepository;
        this.notificacionService = notificacionService;
    }

    @Scheduled(cron = "${app.recordatorio.cron:0 0 8 * * *}")
    public void enviarRecordatorios() {
        LocalDate manana = LocalDate.now().plusDays(1);
        List<Seguimiento> proximos = seguimientoRepository.findPendientesParaRecordatorio(manana);

        if (proximos.isEmpty()) {
            log.info("=== RECORDATORIOS: sin seguimientos programados para mañana ===");
            return;
        }

        log.info("=== RECORDATORIOS: enviando {} recordatorio(s) para {} ===", proximos.size(), manana);
        notificacionService.notificarRecordatorios(proximos);
        log.info("=== RECORDATORIOS: proceso completado ===");
    }
}
