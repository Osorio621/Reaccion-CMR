package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.CambioEtapaRequest;
import com.reactivosdelvalle.crm_api.dto.request.CerrarOportunidadRequest;
import com.reactivosdelvalle.crm_api.dto.request.OportunidadRequest;
import com.reactivosdelvalle.crm_api.dto.response.OportunidadEtapaHistResponse;
import com.reactivosdelvalle.crm_api.dto.response.OportunidadResponse;
import com.reactivosdelvalle.crm_api.service.OportunidadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oportunidades")
public class OportunidadController {

    private final OportunidadService oportunidadService;

    @Autowired
    public OportunidadController(OportunidadService oportunidadService) {
        this.oportunidadService = oportunidadService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OportunidadResponse>> findAll() {
        return ResponseEntity.ok(oportunidadService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OportunidadResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(oportunidadService.findById(id));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OportunidadEtapaHistResponse>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(oportunidadService.historial(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OportunidadResponse> create(@Valid @RequestBody OportunidadRequest request) {
        OportunidadResponse response = oportunidadService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OportunidadResponse> update(@PathVariable Long id, @Valid @RequestBody OportunidadRequest request) {
        return ResponseEntity.ok(oportunidadService.update(id, request));
    }

    @PatchMapping("/{id}/etapa")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OportunidadResponse> cambiarEtapa(@PathVariable Long id, @Valid @RequestBody CambioEtapaRequest request) {
        return ResponseEntity.ok(oportunidadService.cambiarEtapa(id, request));
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OportunidadResponse> cerrar(@PathVariable Long id, @Valid @RequestBody CerrarOportunidadRequest request) {
        return ResponseEntity.ok(oportunidadService.cerrar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        oportunidadService.delete(id);
        return ResponseEntity.ok(Map.of("mensaje", "Oportunidad eliminada correctamente"));
    }
}