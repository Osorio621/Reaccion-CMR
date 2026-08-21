package com.reactivosdelvalle.crm_api.scheduler;

import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Regla de negocio 2: alertas diarias de inactividad.
 * Cada mañana revisa los ejecutivos activos que no registran actividad
 * (login) en más de las horas configuradas y emite la alerta en el log
 * del sistema, dejando constancia de cuándo se alertó a cada usuario.
 */
@Component
public class InactividadScheduler {

    private static final Logger log = LoggerFactory.getLogger(InactividadScheduler.class);

    private final UsuarioRepository usuarioRepository;

    @Value("${app.inactividad.horas:24}")
    private int horasInactividad;

    public InactividadScheduler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Scheduled(cron = "${app.inactividad.cron:0 0 7 * * *}")
    @Transactional
    public void revisarInactividad() {
        LocalDateTime limite = LocalDateTime.now().minusHours(horasInactividad);
        List<Usuario> inactivos =
                usuarioRepository.findActivosSinActividadDesde(RolUsuario.EJECUTIVO, limite);

        if (inactivos.isEmpty()) {
            log.info("=== ALERTA INACTIVIDAD: sin ejecutivos inactivos (> {} h) ===", horasInactividad);
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (Usuario ejecutivo : inactivos) {
            String ultima = ejecutivo.getUltimaActividad() != null
                    ? ejecutivo.getUltimaActividad().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "Nunca ha iniciado sesión";

            log.warn("ALERTA DE INACTIVIDAD: {} {} ({}) - última actividad: {} (> {} h)",
                    ejecutivo.getNombre(), ejecutivo.getApellido(), ejecutivo.getEmail(),
                    ultima, horasInactividad);

            // Constancia de cuándo se emitió la última alerta a este usuario
            ejecutivo.setAlertaInactividad(ahora);
        }

        usuarioRepository.saveAll(inactivos);
        log.info("=== ALERTA INACTIVIDAD: {} ejecutivo(s) notificado(s) ===", inactivos.size());
    }
}
