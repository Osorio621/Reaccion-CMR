package com.reactivosdelvalle.crm_api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportExportRequest {

    @NotNull(message = "El formato es obligatorio")
    private String formato; // csv, json, pdf, xlsx

    @NotNull(message = "El tipo de reporte es obligatorio")
    private String tipo; // ventas, clientes, oportunidades, pipeline, prospectos

    private LocalDate desde;

    private LocalDate hasta;

    private Long ejecutivoId;

    private String estado; // GANADA, PERDIDA, etc.
}