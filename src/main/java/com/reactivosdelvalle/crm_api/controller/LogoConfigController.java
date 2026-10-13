package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.entity.ConfigLogo;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.repository.ConfigLogoRepository;
import com.reactivosdelvalle.crm_api.storage.ArchivoStorage;
import com.reactivosdelvalle.crm_api.util.ValidadorImagen;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Gestión del logo del sistema.
 *
 * Admin:  /api/admin/config/logo  (GET, PUT, POST /upload)
 * Público: /api/config/logo        (GET imagen — sin auth, para <img src>)
 */
@RestController
public class LogoConfigController {

    private static final long MAX_BYTES        = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> MIME_OK   = Set.of(
            "image/png", "image/jpeg", "image/svg+xml");
    private static final String PUBLIC_URL     = "/api/config/logo";
    private static final long   CONFIG_ID      = 1L;

    private final ConfigLogoRepository configLogoRepository;
    private final ArchivoStorage storage;

    public LogoConfigController(ConfigLogoRepository configLogoRepository,
                                ArchivoStorage storage) {
        this.configLogoRepository = configLogoRepository;
        this.storage = storage;
    }

    // ── ADMIN ──────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/config/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> obtener() {
        ConfigLogo config = obtenerConfig();
        String url = resolverUrl(config);
        return ResponseEntity.ok(respuesta(url));
    }

    @PutMapping("/api/admin/config/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> actualizarUrl(
            @RequestBody Map<String, String> body) {

        String url = body.get("url");
        if (url == null || url.isBlank()) {
            throw new AppException("El campo 'url' es obligatorio", HttpStatus.BAD_REQUEST);
        }

        ConfigLogo config = obtenerConfig();
        config.setLogoUrl(url);
        config.setRutaAlmacenada(null); // URL externa → ya no sirve el archivo local
        configLogoRepository.save(config);

        return ResponseEntity.ok(respuesta(url));
    }

    @PostMapping(value = "/api/admin/config/logo/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> subirLogo(
            @RequestParam("logo") MultipartFile archivo) {

        validarImagen(archivo);

        String ruta;
        try {
            ruta = storage.guardar(archivo, "CONFIG", CONFIG_ID);
        } catch (IOException ex) {
            throw new AppException("No se pudo guardar el logo: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ConfigLogo config = obtenerConfig();
        config.setRutaAlmacenada(ruta);
        config.setLogoUrl(null); // archivo local reemplaza URL externa
        configLogoRepository.save(config);

        return ResponseEntity.ok(respuesta(PUBLIC_URL));
    }

    // ── PÚBLICO ────────────────────────────────────────────────────────────

    @GetMapping("/api/config/logo")
    public ResponseEntity<Resource> servirLogo() {
        ConfigLogo config = obtenerConfig();

        if (config.getRutaAlmacenada() == null) {
            return ResponseEntity.notFound().build();
        }

        Resource recurso;
        try {
            recurso = storage.cargar(config.getRutaAlmacenada());
        } catch (IOException ex) {
            return ResponseEntity.notFound().build();
        }

        // Detectar tipo MIME desde la ruta almacenada
        String mime = detectarMime(config.getRutaAlmacenada());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(recurso);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private ConfigLogo obtenerConfig() {
        return configLogoRepository.findById(CONFIG_ID)
                .orElseThrow(() -> new AppException(
                        "Configuración de logo no encontrada", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private String resolverUrl(ConfigLogo config) {
        if (config.getRutaAlmacenada() != null) return PUBLIC_URL;
        if (config.getLogoUrl() != null)        return config.getLogoUrl();
        return null;
    }

    private Map<String, String> respuesta(String url) {
        return Map.of("logoUrl", url != null ? url : "", "url", url != null ? url : "");
    }

    private void validarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new AppException("El archivo está vacío", HttpStatus.BAD_REQUEST);
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new AppException("El logo supera el límite de 5 MB", HttpStatus.BAD_REQUEST);
        }
        // Valida por magic bytes, no por el header Content-Type que puede ser falsificado
        ValidadorImagen.validar(archivo, MIME_OK);
    }

    private String detectarMime(String ruta) {
        String lower = ruta.toLowerCase();
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "image/png";
    }
}
