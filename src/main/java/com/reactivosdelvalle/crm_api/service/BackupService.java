package com.reactivosdelvalle.crm_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    public String ejecutarRespaldo() {
        logger.info("Iniciando respaldo de base de datos PostgreSQL...");
        
        // 1. Parsear URL de conexión
        // Ejemplo: jdbc:postgresql://host:port/database
        if (!datasourceUrl.startsWith("jdbc:postgresql://")) {
            throw new RuntimeException("Solo se admiten respaldos para bases de datos PostgreSQL");
        }

        String cleanUrl = datasourceUrl.substring(18); // Remover "jdbc:postgresql://"
        int slashIndex = cleanUrl.indexOf('/');
        if (slashIndex == -1) {
            throw new RuntimeException("URL de datasource inválida");
        }

        String hostPort = cleanUrl.substring(0, slashIndex);
        String dbName = cleanUrl.substring(slashIndex + 1);

        // Remover posibles parámetros adicionales de la URL de Neon (e.g. ?sslmode=require)
        if (dbName.contains("?")) {
            dbName = dbName.substring(0, dbName.indexOf('?'));
        }

        String host = hostPort;
        String port = "5432";
        if (hostPort.contains(":")) {
            String[] parts = hostPort.split(":");
            host = parts[0];
            port = parts[1];
        }

        // 2. Definir ruta y nombre de archivo con fecha
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String backupDirName = "./backups";
        File backupDir = new File(backupDirName);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String backupFileName = String.format("respaldo_HCG_%s.dump", fechaActual);
        File backupFile = new File(backupDir, backupFileName);
        String backupFilePath = backupFile.getAbsolutePath();

        logger.info("Guardando respaldo en: {}", backupFilePath);

        // 3. Configurar comando pg_dump
        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", port,
                "-U", username,
                "-F", "c", // Formato personalizado (custom archive)
                "-b",      // Incluir blobs
                "-v",      // Verbose
                "-f", backupFilePath,
                dbName
        );

        // Definir la variable PGPASSWORD para evitar que solicite contraseña en consola
        Map<String, String> env = pb.environment();
        env.put("PGPASSWORD", password);

        try {
            Process process = pb.start();

            // Consumir stderr en un hilo separado para evitar deadlock por buffer lleno.
            // Con -v pg_dump escribe mucho; si nadie lee, el OS bloquea el proceso
            // y waitFor() nunca termina.
            Thread stderrConsumer = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.debug("pg_dump: {}", line);
                    }
                } catch (IOException ignored) {}
            });
            stderrConsumer.setDaemon(true);
            stderrConsumer.start();

            boolean termino = process.waitFor(2, TimeUnit.HOURS);
            if (!termino) {
                process.destroyForcibly();
                throw new RuntimeException("pg_dump superó el tiempo límite de 2 horas");
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                logger.info("Respaldo completado con éxito. Archivo: {}", backupFileName);
                return backupFileName;
            } else {
                logger.error("pg_dump falló con código de salida: {}. Verifique que pg_dump esté instalado en el servidor.", exitCode);
                throw new RuntimeException("pg_dump falló con código " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error ejecutando pg_dump para el respaldo: {}", e.getMessage());
            throw new RuntimeException("Error ejecutando pg_dump: " + e.getMessage(), e);
        }
    }
}
