package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.response.ArchivoResponse;
import com.reactivosdelvalle.crm_api.entity.TipoEntidadArchivo;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.service.ArchivoService;
import com.reactivosdelvalle.crm_api.service.ArchivoService.DescargaArchivo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Endpoints de adjuntos.
 *
 * Subir / listar:  /api/{tipo}/{id}/archivos
 *   tipo = clientes | prospectos | oportunidades | visitas
 *
 * Descargar / eliminar: /api/archivos/{archivoId}
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class ArchivoController {

    private final ArchivoService archivoService;

    public ArchivoController(ArchivoService archivoService) {
        this.archivoService = archivoService;
    }

    @PostMapping(value = "/{tipo}/{entidadId}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoResponse> subir(
            @PathVariable String tipo,
            @PathVariable Long entidadId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "descripcion", required = false) String descripcion) {

        TipoEntidadArchivo tipoEnum = parsearTipo(tipo);
        ArchivoResponse respuesta = archivoService.subir(tipoEnum, entidadId, archivo, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{tipo}/{entidadId}/archivos")
    public ResponseEntity<List<ArchivoResponse>> listar(
            @PathVariable String tipo,
            @PathVariable Long entidadId) {

        TipoEntidadArchivo tipoEnum = parsearTipo(tipo);
        return ResponseEntity.ok(archivoService.listar(tipoEnum, entidadId));
    }

    @GetMapping("/archivos/{archivoId}/descargar")
    public ResponseEntity<org.springframework.core.io.Resource> descargar(
            @PathVariable Long archivoId) {

        DescargaArchivo descarga = archivoService.descargar(archivoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(descarga.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + descarga.nombreOriginal() + "\"")
                .body(descarga.recurso());
    }

    @DeleteMapping("/archivos/{archivoId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long archivoId) {
        archivoService.eliminar(archivoId);
        return ResponseEntity.noContent().build();
    }

    // Mapea el segmento URL al enum (clientes → CLIENTE, etc.)
    private TipoEntidadArchivo parsearTipo(String tipo) {
        return switch (tipo.toLowerCase()) {
            case "clientes"     -> TipoEntidadArchivo.CLIENTE;
            case "prospectos"   -> TipoEntidadArchivo.PROSPECTO;
            case "oportunidades"-> TipoEntidadArchivo.OPORTUNIDAD;
            case "visitas"      -> TipoEntidadArchivo.VISITA;
            default -> throw new AppException(
                    "Entidad no soportada: " + tipo + ". Use: clientes, prospectos, oportunidades, visitas",
                    HttpStatus.BAD_REQUEST);
        };
    }
}
