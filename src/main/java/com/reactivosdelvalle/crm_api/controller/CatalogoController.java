package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.CatalogoRequest;
import com.reactivosdelvalle.crm_api.dto.response.CatalogoResponse;
import com.reactivosdelvalle.crm_api.service.CatalogoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/catalogos")
@PreAuthorize("isAuthenticated()")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @Autowired
    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public ResponseEntity<List<CatalogoResponse>> findAll(@RequestParam(required = false) String tipo) {
        if (tipo != null && !tipo.isBlank()) {
            return ResponseEntity.ok(catalogoService.findByTipo(tipo));
        }
        return ResponseEntity.ok(catalogoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogoResponse> create(@Valid @RequestBody CatalogoRequest request) {
        CatalogoResponse response = catalogoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogoResponse> update(@PathVariable Long id, @Valid @RequestBody CatalogoRequest request) {
        return ResponseEntity.ok(catalogoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        catalogoService.delete(id);
        return ResponseEntity.ok(Map.of("mensaje", "Catálogo eliminado correctamente"));
    }
}