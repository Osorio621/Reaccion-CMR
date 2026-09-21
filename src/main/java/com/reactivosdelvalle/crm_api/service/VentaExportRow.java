package com.reactivosdelvalle.crm_api.service;

public record VentaExportRow(
        String periodo,
        String ejecutivo,
        double meta,
        double ventaReal,
        double forecast,
        String cumplimiento
) {}