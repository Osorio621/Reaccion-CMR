package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.response.PipelineEtapaResponse;
import com.reactivosdelvalle.crm_api.service.OportunidadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    private final OportunidadService oportunidadService;

    @Autowired
    public PipelineController(OportunidadService oportunidadService) {
        this.oportunidadService = oportunidadService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PipelineEtapaResponse>> pipeline() {
        return ResponseEntity.ok(oportunidadService.pipeline());
    }
}