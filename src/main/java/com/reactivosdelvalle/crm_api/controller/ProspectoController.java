package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.ConvertirProspectoRequest;
import com.reactivosdelvalle.crm_api.dto.request.ProspectoRequest;
import com.reactivosdelvalle.crm_api.dto.response.ClienteResponse;
import com.reactivosdelvalle.crm_api.dto.response.ConvertirProspectoResponse;
import com.reactivosdelvalle.crm_api.dto.response.ProspectoResponse;
import com.reactivosdelvalle.crm_api.service.ProspectoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prospectos")
public class ProspectoController {

    private final ProspectoService prospectoService;

    @Autowired
    public ProspectoController(ProspectoService prospectoService) {
        this.prospectoService = prospectoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProspectoResponse>> findAll() {
        return ResponseEntity.ok(prospectoService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProspectoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(prospectoService.findById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProspectoResponse> create(@Valid @RequestBody ProspectoRequest request) {
        ProspectoResponse response = prospectoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProspectoResponse> update(@PathVariable Long id, @Valid @RequestBody ProspectoRequest request) {
        return ResponseEntity.ok(prospectoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        prospectoService.delete(id);
        return ResponseEntity.ok(Map.of("mensaje", "Prospecto eliminado correctamente"));
    }

    @PostMapping("/{id}/convertir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConvertirProspectoResponse> convertir(@PathVariable Long id, @Valid @RequestBody ConvertirProspectoRequest request) {
        ConvertirProspectoResponse response = prospectoService.convertir(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}