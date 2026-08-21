package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.CerrarSeguimientoRequest;
import com.reactivosdelvalle.crm_api.dto.request.SeguimientoRequest;
import com.reactivosdelvalle.crm_api.dto.response.SeguimientoResponse;
import com.reactivosdelvalle.crm_api.service.SeguimientoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seguimientos")
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    @Autowired
    public SeguimientoController(SeguimientoService seguimientoService) {
        this.seguimientoService = seguimientoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SeguimientoResponse>> listar() {
        return ResponseEntity.ok(seguimientoService.listar());
    }

    @GetMapping("/vencidos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SeguimientoResponse>> listarVencidos() {
        return ResponseEntity.ok(seguimientoService.listarVencidos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeguimientoResponse> ver(@PathVariable Long id) {
        return ResponseEntity.ok(seguimientoService.ver(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeguimientoResponse> crear(@Valid @RequestBody SeguimientoRequest request) {
        return ResponseEntity.ok(seguimientoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeguimientoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody SeguimientoRequest request) {
        return ResponseEntity.ok(seguimientoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SeguimientoResponse> cerrar(
            @PathVariable Long id, 
            @RequestBody CerrarSeguimientoRequest request) {
        return ResponseEntity.ok(seguimientoService.cerrar(id, request.getNotas(), request.getProximaAccion()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        seguimientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
