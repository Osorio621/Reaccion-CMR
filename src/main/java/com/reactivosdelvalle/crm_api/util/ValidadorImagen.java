package com.reactivosdelvalle.crm_api.util;

import com.reactivosdelvalle.crm_api.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Valida imágenes por magic bytes, no por el header Content-Type que envía el cliente.
 * Impide subir archivos maliciosos (JSP, PHP, etc.) con MIME falsificado.
 */
public final class ValidadorImagen {

    private static final byte[] MAGIC_PNG  = { (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] MAGIC_JPEG = { (byte)0xFF, (byte)0xD8, (byte)0xFF };

    private ValidadorImagen() {}

    /**
     * Verifica que el contenido real del archivo corresponda a un tipo de imagen permitido.
     * Lanza AppException 400 si el contenido no coincide con ningún tipo de la lista.
     *
     * @param archivo        archivo multipart recibido
     * @param mimesPermitidos tipos MIME aceptados, ej. {@code Set.of("image/png","image/jpeg")}
     */
    public static void validar(MultipartFile archivo, Set<String> mimesPermitidos) {
        if (archivo == null || archivo.isEmpty()) {
            throw new AppException("El archivo está vacío", HttpStatus.BAD_REQUEST);
        }

        String mimeDetectado;
        try (InputStream is = archivo.getInputStream()) {
            byte[] cabecera = is.readNBytes(200);
            mimeDetectado = detectarMime(cabecera);
        } catch (IOException ex) {
            throw new AppException("No se pudo leer el archivo", HttpStatus.BAD_REQUEST);
        }

        if (mimeDetectado == null || !mimesPermitidos.contains(mimeDetectado)) {
            throw new AppException(
                    "El contenido del archivo no corresponde a un formato permitido. " +
                    "Formatos aceptados: " + String.join(", ", mimesPermitidos),
                    HttpStatus.BAD_REQUEST, "FORMATO_NO_PERMITIDO");
        }
    }

    private static String detectarMime(byte[] cabecera) {
        if (cabecera.length >= 8 && empieza(cabecera, MAGIC_PNG))  return "image/png";
        if (cabecera.length >= 3 && empieza(cabecera, MAGIC_JPEG)) return "image/jpeg";
        if (esSvg(cabecera))                                        return "image/svg+xml";
        return null;
    }

    private static boolean empieza(byte[] datos, byte[] magic) {
        for (int i = 0; i < magic.length; i++) {
            if (datos[i] != magic[i]) return false;
        }
        return true;
    }

    /** SVG es texto XML — acepta BOM UTF-8 opcional antes de la declaración. */
    private static boolean esSvg(byte[] cabecera) {
        int offset = 0;
        if (cabecera.length >= 3
                && (cabecera[0] & 0xFF) == 0xEF
                && (cabecera[1] & 0xFF) == 0xBB
                && (cabecera[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        int longitud = Math.min(100, cabecera.length - offset);
        if (longitud <= 0) return false;
        String texto = new String(cabecera, offset, longitud, StandardCharsets.UTF_8)
                .stripLeading()
                .toLowerCase();
        return texto.startsWith("<?xml") || texto.startsWith("<svg");
    }
}
