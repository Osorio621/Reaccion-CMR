package com.reactivosdelvalle.crm_api.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Abstracción de almacenamiento de archivos.
 * La implementación activa se elige con app.storage.tipo (local | s3).
 */
public interface ArchivoStorage {

    /**
     * Guarda el archivo y retorna la ruta relativa dentro del directorio base.
     * Ej: "CLIENTE/42/a3f8c2d1_contrato.pdf"
     */
    String guardar(MultipartFile archivo, String entidadTipo, Long entidadId) throws IOException;

    /** Elimina el archivo dado su ruta relativa. */
    void eliminar(String rutaRelativa) throws IOException;

    /** Carga el archivo como recurso descargable. */
    Resource cargar(String rutaRelativa) throws IOException;
}
