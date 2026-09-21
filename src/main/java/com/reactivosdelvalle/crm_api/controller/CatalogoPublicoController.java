package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.response.CatalogoResponse;
import com.reactivosdelvalle.crm_api.mapper.CatalogoMapper;
import com.reactivosdelvalle.crm_api.repository.CatalogoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint público de lectura de catálogos (cualquier usuario autenticado).
 * Solo lectura — el CRUD administrativo sigue en /api/admin/catalogos.
 */
@RestController
@RequestMapping("/api/catalogos")
@PreAuthorize("isAuthenticated()")
public class CatalogoPublicoController {

    private final CatalogoRepository catalogoRepository;
    private final CatalogoMapper catalogoMapper;

    public CatalogoPublicoController(CatalogoRepository catalogoRepository,
                                     CatalogoMapper catalogoMapper) {
        this.catalogoRepository = catalogoRepository;
        this.catalogoMapper = catalogoMapper;
    }

    @GetMapping
    public ResponseEntity<List<CatalogoResponse>> listar(
            @RequestParam(required = false) String tipo) {

        List<CatalogoResponse> resultado;

        if (tipo != null && !tipo.isBlank()) {
            resultado = catalogoRepository
                    .findByTipoAndActivoTrueOrderByOrdenAsc(tipo)
                    .stream()
                    .map(catalogoMapper::toResponse)
                    .toList();
        } else {
            resultado = catalogoRepository
                    .findAllByOrderByTipoAscOrdenAsc()
                    .stream()
                    .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                    .map(catalogoMapper::toResponse)
                    .toList();
        }

        return ResponseEntity.ok(resultado);
    }
}
