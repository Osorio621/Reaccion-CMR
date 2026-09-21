package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.ComunicacionRequest;
import com.reactivosdelvalle.crm_api.dto.response.ComunicacionResponse;
import com.reactivosdelvalle.crm_api.entity.TipoEntidadComunicacion;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.service.ComunicacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de historial de comunicaciones.
 *
 * Registrar / listar: /api/{tipo}/{entidadId}/comunicaciones
 *   tipo = clientes | prospectos
 *
 * Actualizar / eliminar: /api/comunicaciones/{id}
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class ComunicacionController {

    private final ComunicacionService comunicacionService;

    public ComunicacionController(ComunicacionService comunicacionService) {
        this.comunicacionService = comunicacionService;
    }

    @PostMapping("/{tipo}/{entidadId}/comunicaciones")
    public ResponseEntity<ComunicacionResponse> registrar(
            @PathVariable String tipo,
            @PathVariable Long entidadId,
            @Valid @RequestBody ComunicacionRequest request) {

        TipoEntidadComunicacion tipoEnum = parsearTipo(tipo);
        ComunicacionResponse respuesta = comunicacionService.registrar(tipoEnum, entidadId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{tipo}/{entidadId}/comunicaciones")
    public ResponseEntity<List<ComunicacionResponse>> listar(
            @PathVariable String tipo,
            @PathVariable Long entidadId) {

        TipoEntidadComunicacion tipoEnum = parsearTipo(tipo);
        return ResponseEntity.ok(comunicacionService.listar(tipoEnum, entidadId));
    }

    @PutMapping("/comunicaciones/{id}")
    public ResponseEntity<ComunicacionResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ComunicacionRequest request) {

        return ResponseEntity.ok(comunicacionService.actualizar(id, request));
    }

    @DeleteMapping("/comunicaciones/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        comunicacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private TipoEntidadComunicacion parsearTipo(String tipo) {
        return switch (tipo.toLowerCase()) {
            case "clientes"   -> TipoEntidadComunicacion.CLIENTE;
            case "prospectos" -> TipoEntidadComunicacion.PROSPECTO;
            default -> throw new AppException(
                    "Entidad no soportada: " + tipo + ". Use: clientes, prospectos",
                    HttpStatus.BAD_REQUEST);
        };
    }
}
