package com.reactivosdelvalle.crm_api.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.tipo", havingValue = "local", matchIfMissing = true)
public class LocalStorage implements ArchivoStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalStorage.class);

    @Value("${app.storage.ruta-local:./uploads}")
    private String rutaBase;

    private Path dirBase;

    @PostConstruct
    void init() throws IOException {
        dirBase = Paths.get(rutaBase).toAbsolutePath().normalize();
        Files.createDirectories(dirBase);
        log.info("Storage local inicializado en: {}", dirBase);
    }

    @Override
    public String guardar(MultipartFile archivo, String entidadTipo, Long entidadId) throws IOException {
        // Sanitizar nombre original: quitar caracteres problemáticos
        String nombreOriginal = Paths.get(archivo.getOriginalFilename()).getFileName().toString();
        String nombreSanitizado = nombreOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");

        String nombreAlmacenado = UUID.randomUUID().toString().replace("-", "") + "_" + nombreSanitizado;

        Path directorio = dirBase.resolve(entidadTipo).resolve(String.valueOf(entidadId));
        Files.createDirectories(directorio);

        Path destino = directorio.resolve(nombreAlmacenado);
        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        // Retornar ruta relativa separada por /
        return entidadTipo + "/" + entidadId + "/" + nombreAlmacenado;
    }

    @Override
    public void eliminar(String rutaRelativa) throws IOException {
        Path archivo = dirBase.resolve(rutaRelativa).normalize();
        // Evitar path traversal
        if (!archivo.startsWith(dirBase)) {
            throw new IOException("Ruta fuera del directorio base: " + rutaRelativa);
        }
        Files.deleteIfExists(archivo);
    }

    @Override
    public Resource cargar(String rutaRelativa) throws IOException {
        Path archivo = dirBase.resolve(rutaRelativa).normalize();
        if (!archivo.startsWith(dirBase)) {
            throw new IOException("Ruta fuera del directorio base: " + rutaRelativa);
        }
        Resource recurso = new UrlResource(archivo.toUri());
        if (!recurso.exists() || !recurso.isReadable()) {
            throw new IOException("Archivo no encontrado o no legible: " + rutaRelativa);
        }
        return recurso;
    }
}
