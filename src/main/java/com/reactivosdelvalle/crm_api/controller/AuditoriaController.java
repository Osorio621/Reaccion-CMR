package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.response.AuditoriaResponse;
import com.reactivosdelvalle.crm_api.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @Autowired
    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    /**
     * Últimos movimientos de auditoría (por defecto 100, máximo 500).
     */
    @GetMapping
    public ResponseEntity<List<AuditoriaResponse>> listarUltimos(
            @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(auditoriaService.listarUltimos(limite));
    }

    /**
     * Historial de cambios de un registro concreto (ej: tabla=clientes, registroId=5).
     */
    @GetMapping("/tabla/{tabla}/registro/{registroId}")
    public ResponseEntity<List<AuditoriaResponse>> historialRegistro(
            @PathVariable String tabla,
            @PathVariable Long registroId) {
        return ResponseEntity.ok(auditoriaService.historialRegistro(tabla, registroId));
    }

    /**
     * Movimientos realizados por un usuario (quién hizo qué).
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<AuditoriaResponse>> porUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(auditoriaService.porUsuario(usuarioId, limite));
    }
}
