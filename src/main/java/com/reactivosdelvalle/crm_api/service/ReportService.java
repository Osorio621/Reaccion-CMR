package com.reactivosdelvalle.crm_api.service;

import com.opencsv.CSVWriter;
import com.reactivosdelvalle.crm_api.dto.request.ReportExportRequest;
import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.repository.*;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ClienteRepository clienteRepository;
    private final OportunidadRepository oportunidadRepository;
    private final OportunidadEtapaHistRepository etapaHistRepository;
    private final ProspectoRepository prospectoRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoRepository catalogoRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    public ReportService(ClienteRepository clienteRepository,
                         OportunidadRepository oportunidadRepository,
                         OportunidadEtapaHistRepository etapaHistRepository,
                         ProspectoRepository prospectoRepository,
                         VentaRepository ventaRepository,
                         UsuarioRepository usuarioRepository,
                         CatalogoRepository catalogoRepository,
                         SecurityUtils securityUtils) {
        this.clienteRepository = clienteRepository;
        this.oportunidadRepository = oportunidadRepository;
        this.etapaHistRepository = etapaHistRepository;
        this.prospectoRepository = prospectoRepository;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoRepository = catalogoRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public byte[] exportar(ReportExportRequest request) {
        String formato = request.getFormato().toLowerCase();
        String tipo = request.getTipo().toLowerCase();

        List<?> datos = obtenerDatos(tipo, request);

        return switch (formato) {
            case "csv" -> generarCsv(datos);
            case "json" -> generarJson(datos);
            case "xlsx" -> generarXlsx(datos, tipo);
            default -> throw new IllegalArgumentException("Formato no soportado: " + formato + ". Use: csv, json, xlsx");
        };
    }

    private List<?> obtenerDatos(String tipo, ReportExportRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        boolean esEjecutivo = usuario != null && usuario.getRol() == RolUsuario.EJECUTIVO;
        Long ejecutivoId = request.getEjecutivoId();

        if (esEjecutivo) {
            ejecutivoId = usuario.getId();
        }

        return switch (tipo) {
            case "ventas" -> obtenerVentas(request.getDesde(), request.getHasta(), ejecutivoId);
            case "clientes" -> obtenerClientes(request.getDesde(), request.getHasta(), ejecutivoId);
            case "oportunidades" -> obtenerOportunidades(request.getDesde(), request.getHasta(), ejecutivoId, request.getEstado());
            case "pipeline" -> obtenerPipeline(ejecutivoId);
            case "prospectos" -> obtenerProspectos(request.getDesde(), request.getHasta(), ejecutivoId);
            default -> throw new IllegalArgumentException("Tipo de reporte no soportado: " + tipo);
        };
    }

    private List<VentaExportRow> obtenerVentas(LocalDate desde, LocalDate hasta, Long ejecutivoId) {
        LocalDate fdesde = desde != null ? desde : LocalDate.of(2000, 1, 1);
        LocalDate fhasta = hasta != null ? hasta : LocalDate.now();
        List<Object[]> resultados;
        if (ejecutivoId != null) {
            resultados = ventaRepository.findByEjecutivoIdAndPeriodo(ejecutivoId, fdesde, fhasta);
        } else {
            resultados = ventaRepository.findByPeriodo(fdesde, fhasta);
        }
        return resultados.stream().map(this::mapVentaRow).collect(Collectors.toList());
    }

    private VentaExportRow mapVentaRow(Object[] row) {
        return new VentaExportRow(
                row[0] != null ? row[0].toString() : "",
                row[1] != null ? row[1].toString() : "",
                row[2] instanceof Number ? ((Number) row[2]).doubleValue() : 0,
                row[3] instanceof Number ? ((Number) row[3]).doubleValue() : 0,
                row[4] instanceof Number ? ((Number) row[4]).doubleValue() : 0,
                row[2] instanceof Number && ((Number) row[2]).doubleValue() > 0
                        ? String.format("%.1f%%", (((Number) row[3]).doubleValue() / ((Number) row[2]).doubleValue()) * 100)
                        : "0%"
        );
    }

    private List<ClienteExportRow> obtenerClientes(LocalDate desde, LocalDate hasta, Long ejecutivoId) {
        LocalDate fdesde = desde != null ? desde : LocalDate.of(2000, 1, 1);
        LocalDate fhasta = hasta != null ? hasta : LocalDate.now();
        List<Object[]> resultados;
        if (ejecutivoId != null) {
            resultados = clienteRepository.findExportByEjecutivoIdAndFecha(ejecutivoId, fdesde, fhasta);
        } else {
            resultados = clienteRepository.findExportByFecha(fdesde, fhasta);
        }
        return resultados.stream().map(r -> new ClienteExportRow(
                str(r, 0), str(r, 1), str(r, 2), str(r, 3),
                str(r, 4), str(r, 5), str(r, 6)
        )).collect(Collectors.toList());
    }

    private List<OportunidadExportRow> obtenerOportunidades(LocalDate desde, LocalDate hasta, Long ejecutivoId, String estado) {
        LocalDate fdesde = desde != null ? desde : LocalDate.of(2000, 1, 1);
        LocalDate fhasta = hasta != null ? hasta : LocalDate.now();
        List<Object[]> resultados;
        if (ejecutivoId != null && estado != null) {
            resultados = oportunidadRepository.findExportByEjecutivoIdAndEstadoAndFecha(ejecutivoId, estado, fdesde, fhasta);
        } else if (ejecutivoId != null) {
            resultados = oportunidadRepository.findExportByEjecutivoIdAndFecha(ejecutivoId, fdesde, fhasta);
        } else if (estado != null) {
            resultados = oportunidadRepository.findExportByEstadoAndFecha(estado, fdesde, fhasta);
        } else {
            resultados = oportunidadRepository.findExportByFecha(fdesde, fhasta);
        }
        return resultados.stream().map(r -> new OportunidadExportRow(
                str(r, 0), str(r, 1), str(r, 2),
                r[3] instanceof Number ? BigDecimal.valueOf(((Number) r[3]).doubleValue()) : BigDecimal.ZERO,
                r[4] instanceof Number ? ((Number) r[4]).intValue() : 0,
                str(r, 5),
                r[6] != null ? LocalDate.parse(r[6].toString().substring(0, 10)) : null
        )).collect(Collectors.toList());
    }

    private List<PipelineExportRow> obtenerPipeline(Long ejecutivoId) {
        List<Object[]> resultados;
        if (ejecutivoId != null) {
            resultados = oportunidadRepository.getPipelineByEjecutivo(ejecutivoId);
        } else {
            resultados = oportunidadRepository.getPipelineGlobal();
        }
        return resultados.stream().map(r -> new PipelineExportRow(
                str(r, 0),
                r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L,
                r[2] instanceof Number ? BigDecimal.valueOf(((Number) r[2]).doubleValue()) : BigDecimal.ZERO,
                r[3] instanceof Number ? BigDecimal.valueOf(((Number) r[3]).doubleValue()) : BigDecimal.ZERO
        )).collect(Collectors.toList());
    }

    private List<ProspectoExportRow> obtenerProspectos(LocalDate desde, LocalDate hasta, Long ejecutivoId) {
        LocalDate fdesde = desde != null ? desde : LocalDate.of(2000, 1, 1);
        LocalDate fhasta = hasta != null ? hasta : LocalDate.now();
        List<Object[]> resultados;
        if (ejecutivoId != null) {
            resultados = prospectoRepository.findExportByResponsableIdAndFecha(ejecutivoId, fdesde, fhasta);
        } else {
            resultados = prospectoRepository.findExportByFecha(fdesde, fhasta);
        }
        return resultados.stream().map(r -> new ProspectoExportRow(
                str(r, 0), str(r, 1), str(r, 2),
                str(r, 3), str(r, 4),
                r[5] != null ? r[5].toString().substring(0, 10) : ""
        )).collect(Collectors.toList());
    }

    private String str(Object[] row, int index) {
        return row[index] != null ? row[index].toString() : "";
    }

    private byte[] generarCsv(List<?> datos) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos);
             CSVWriter csvWriter = new CSVWriter(writer)) {
            String[] headers = obtenerHeaders(obtenerTipo(datos));
            csvWriter.writeNext(headers);
            String tipo = obtenerTipo(datos);
            for (Object obj : datos) {
                csvWriter.writeNext(extraerValores(obj, tipo));
            }
            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando CSV", e);
            throw new RuntimeException("Error generando CSV: " + e.getMessage());
        }
    }

    private String obtenerTipo(List<?> datos) {
        if (datos.isEmpty()) return "";
        Object first = datos.get(0);
        if (first instanceof VentaExportRow) return "ventas";
        if (first instanceof ClienteExportRow) return "clientes";
        if (first instanceof OportunidadExportRow) return "oportunidades";
        if (first instanceof PipelineExportRow) return "pipeline";
        if (first instanceof ProspectoExportRow) return "prospectos";
        return "";
    }

    private byte[] generarJson(List<?> datos) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.writeValueAsBytes(datos);
        } catch (Exception e) {
            log.error("Error generando JSON", e);
            throw new RuntimeException("Error generando JSON: " + e.getMessage());
        }
    }

    private byte[] generarXlsx(List<?> datos, String tipo) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(capitalize(tipo));

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setWrapText(true);

            String[] headers = obtenerHeaders(tipo);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Object obj : datos) {
                Row row = sheet.createRow(rowNum++);
                String[] values = extraerValores(obj, tipo);
                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(values[i] != null ? values[i] : "");
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando XLSX", e);
            throw new RuntimeException("Error generando XLSX: " + e.getMessage());
        }
    }

    private String[] obtenerHeaders(String tipo) {
        return switch (tipo) {
            case "ventas" -> new String[]{"Periodo", "Ejecutivo", "Meta", "Venta Real", "Forecast", "Cumplimiento"};
            case "clientes" -> new String[]{"Nombre", "Empresa", "Email", "Teléfono", "Ejecutivo", "Industria", "Zona"};
            case "oportunidades" -> new String[]{"Nombre", "Cliente", "Etapa", "Valor", "Probabilidad", "Estado", "Fecha Cierre"};
            case "pipeline" -> new String[]{"Etapa", "Cantidad Oportunidades", "Valor Total", "Valor Ponderado"};
            case "prospectos" -> new String[]{"Nombre", "Empresa", "Email", "Etapa", "Origen", "Fecha Creación"};
            default -> new String[]{};
        };
    }

    private String[] extraerValores(Object obj, String tipo) {
        if (obj instanceof VentaExportRow v) {
            return new String[]{v.periodo(), v.ejecutivo(),
                    String.valueOf(v.meta()), String.valueOf(v.ventaReal()),
                    String.valueOf(v.forecast()), v.cumplimiento()};
        }
        if (obj instanceof ClienteExportRow c) {
            return new String[]{c.nombre(), c.razonSocial(), c.email(),
                    c.telefono(), c.ejecutivo(), c.industria(), c.zona()};
        }
        if (obj instanceof OportunidadExportRow o) {
            return new String[]{o.nombre(), o.cliente(), o.etapa(),
                    String.valueOf(o.valor()), String.valueOf(o.probabilidad()) + "%",
                    o.estado(), o.fechaCierre() != null ? o.fechaCierre().toString() : ""};
        }
        if (obj instanceof PipelineExportRow p) {
            return new String[]{p.etapa(), String.valueOf(p.cantidad()),
                    String.valueOf(p.valorTotal()), String.valueOf(p.valorPonderado())};
        }
        if (obj instanceof ProspectoExportRow pr) {
            return new String[]{pr.nombre(), pr.empresa(), pr.email(),
                    pr.etapa(), pr.origen(), pr.fechaCreacion()};
        }
        return new String[]{};
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}