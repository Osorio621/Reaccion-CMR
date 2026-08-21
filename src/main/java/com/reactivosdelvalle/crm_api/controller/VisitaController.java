package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.VisitaRequest;
import com.reactivosdelvalle.crm_api.dto.response.VisitaResponse;
import com.reactivosdelvalle.crm_api.service.VisitaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visitas")
public class VisitaController {

    private final VisitaService visitaService;

    @Autowired
    public VisitaController(VisitaService visitaService) {
        this.visitaService = visitaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VisitaResponse>> listar() {
        return ResponseEntity.ok(visitaService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VisitaResponse> ver(@PathVariable Long id) {
        return ResponseEntity.ok(visitaService.ver(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VisitaResponse> crear(@Valid @RequestBody VisitaRequest request) {
        return ResponseEntity.ok(visitaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VisitaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody VisitaRequest request) {
        return ResponseEntity.ok(visitaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        visitaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
