package com.reactivosdelvalle.crm_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private BigDecimal valorTotalPipeline;
    private BigDecimal valorPonderadoPipeline;
    private BigDecimal tasaConversion;
    private Long oportunidadesCierreMes;
    private Long seguimientosVencidos;
    private List<ExecutiveVisitCount> visitasUltimos7Dias;
    private List<ExecutiveSalesPerformance> cumplimientoMensual;
    private List<InactiveExecutive> alertasInactividad;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutiveVisitCount {
        private Long ejecutivoId;
        private String ejecutivoNombre;
        private Long cantidadVisitas;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutiveSalesPerformance {
        private Long ejecutivoId;
        private String ejecutivoNombre;
        private BigDecimal meta;
        private BigDecimal ventaReal;
        private BigDecimal forecast;
        private BigDecimal porcentajeCumplimiento;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InactiveExecutive {
        private Long ejecutivoId;
        private String ejecutivoNombre;
        private String ultimaActividad;
    }
}
