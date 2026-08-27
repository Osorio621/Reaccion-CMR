package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.ReportExportRequest;
import com.reactivosdelvalle.crm_api.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("isAuthenticated()")
@Validated
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportar(
            @Valid ReportExportRequest request) {

        String formato = request.getFormato().toLowerCase();
        if (!List.of("csv", "json", "xlsx", "pdf").contains(formato)) {
            throw new IllegalArgumentException("Formato no soportado: " + formato + ". Use: csv, json, xlsx, pdf");
        }

        byte[] contenido = reportService.exportar(request);

        String nombreArchivo = String.format("%s_%s.%s",
                request.getTipo(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                formato);

        MediaType mediaType = switch (formato) {
            case "csv" -> MediaType.parseMediaType("text/csv");
            case "json" -> MediaType.APPLICATION_JSON;
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "pdf" -> MediaType.parseMediaType("application/pdf");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(mediaType)
                .contentLength(contenido.length)
                .body(new ByteArrayResource(contenido));
    }
}