package com.reactivosdelvalle.crm_api.scheduler;

import com.reactivosdelvalle.crm_api.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Regla de negocio 11: respaldo semanal automático de la base de datos.
 * Ejecuta pg_dump los domingos a la 1:00 AM y limpia respaldos antiguos
 * según la retención configurada.
 */
@Component
public class RespaldoScheduler {

    private static final Logger log = LoggerFactory.getLogger(RespaldoScheduler.class);
    private static final String DIRECTORIO_RESPALDOS = "./backups";

    private final BackupService backupService;

    @Value("${app.respaldo.retencion-dias:30}")
    private int retencionDias;

    public RespaldoScheduler(BackupService backupService) {
        this.backupService = backupService;
    }

    @Scheduled(cron = "${app.respaldo.cron:0 0 1 * * SUN}")
    public void ejecutarRespaldoSemanal() {
        log.info("=== RESPALDO SEMANAL: inicio ===");
        try {
            String archivo = backupService.ejecutarRespaldo();
            log.info("=== RESPALDO SEMANAL: completado ({}) ===", archivo);
        } catch (Exception ex) {
            // El scheduler nunca debe morir por un fallo del respaldo
            log.error("=== RESPALDO SEMANAL: FALLÓ - {} ===", ex.getMessage());
        }
        limpiarRespaldosAntiguos();
    }

    /**
     * Elimina archivos .dump con más días de antigüedad que la retención
     * configurada, para que el directorio de respaldos no crezca sin límite.
     */
    private void limpiarRespaldosAntiguos() {
        File directorio = new File(DIRECTORIO_RESPALDOS);
        File[] respaldos = directorio.listFiles((dir, name) -> name.endsWith(".dump"));
        if (respaldos == null || respaldos.length == 0) {
            return;
        }

        LocalDateTime limite = LocalDateTime.now().minusDays(retencionDias);
        int eliminados = 0;

        for (File respaldo : respaldos) {
            try {
                LocalDateTime fechaArchivo = LocalDateTime.parse(
                        extraerMarcaDeTiempo(respaldo.getName()),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                if (fechaArchivo.isBefore(limite)) {
                    if (Files.deleteIfExists(Path.of(respaldo.getAbsolutePath()))) {
                        eliminados++;
                        log.info("Respaldo antiguo eliminado (retención {} días): {}", retencionDias, respaldo.getName());
                    }
                }
            } catch (Exception ex) {
                log.warn("No se pudo evaluar/eliminar el respaldo {}: {}", respaldo.getName(), ex.getMessage());
            }
        }

        if (eliminados > 0) {
            log.info("Limpieza de respaldos: {} archivo(s) eliminado(s)", eliminados);
        }
    }

    private String extraerMarcaDeTiempo(String nombreArchivo) {
        // Formato: respaldo_HCG_yyyy-MM-dd_HH-mm-ss.dump
        String sinExtension = nombreArchivo.substring(0, nombreArchivo.length() - ".dump".length());
        return sinExtension.substring(sinExtension.lastIndexOf('_') + 1);
    }
}
