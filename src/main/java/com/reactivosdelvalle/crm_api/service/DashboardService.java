package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.response.DashboardResponse;
import com.reactivosdelvalle.crm_api.entity.*;
import com.reactivosdelvalle.crm_api.repository.*;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final OportunidadRepository oportunidadRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final VisitaRepository visitaRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    public DashboardService(
            OportunidadRepository oportunidadRepository,
            SeguimientoRepository seguimientoRepository,
            VisitaRepository visitaRepository,
            VentaRepository ventaRepository,
            UsuarioRepository usuarioRepository,
            SecurityUtils securityUtils) {
        this.oportunidadRepository = oportunidadRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.visitaRepository = visitaRepository;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumen() {
        UsuarioPrincipal usuarioActual = securityUtils.getUsuarioActual();
        boolean esEjecutivo = (usuarioActual != null && usuarioActual.getRol() == RolUsuario.EJECUTIVO);
        Long ejecutivoId = esEjecutivo ? usuarioActual.getId() : null;

        LocalDate hoy = LocalDate.now();

        // 1 & 2. Valor Total y Ponderado del Pipeline (Oportunidades Activas)
        List<Oportunidad> activas;
        if (esEjecutivo) {
            activas = oportunidadRepository.findByEjecutivoIdAndActivoTrueAndEstadoOrderByUpdatedAtDesc(ejecutivoId, EstadoOportunidad.ACTIVA);
        } else {
            activas = oportunidadRepository.findAllByActivoTrueAndEstadoOrderByUpdatedAtDesc(EstadoOportunidad.ACTIVA);
        }

        BigDecimal valorTotal = BigDecimal.ZERO;
        BigDecimal valorPonderado = BigDecimal.ZERO;

        for (Oportunidad op : activas) {
            BigDecimal valor = op.getValor() != null ? op.getValor() : BigDecimal.ZERO;
            valorTotal = valorTotal.add(valor);
            
            BigDecimal ponderado = op.getValorPonderado() != null ? op.getValorPonderado() : BigDecimal.ZERO;
            valorPonderado = valorPonderado.add(ponderado);
        }

        // 3. Tasa de Conversión: ganadas / (ganadas + perdidas) * 100
        List<Oportunidad> todasOps;
        if (esEjecutivo) {
            todasOps = oportunidadRepository.findByEjecutivoIdAndActivoTrueAndEstadoOrderByUpdatedAtDesc(ejecutivoId, EstadoOportunidad.GANADA);
            todasOps.addAll(oportunidadRepository.findByEjecutivoIdAndActivoTrueAndEstadoOrderByUpdatedAtDesc(ejecutivoId, EstadoOportunidad.PERDIDA));
        } else {
            todasOps = oportunidadRepository.findAllByActivoTrueAndEstadoOrderByUpdatedAtDesc(EstadoOportunidad.GANADA);
            todasOps.addAll(oportunidadRepository.findAllByActivoTrueAndEstadoOrderByUpdatedAtDesc(EstadoOportunidad.PERDIDA));
        }

        long ganadas = todasOps.stream().filter(o -> o.getEstado() == EstadoOportunidad.GANADA).count();
        long perdidas = todasOps.stream().filter(o -> o.getEstado() == EstadoOportunidad.PERDIDA).count();
        long totalCerradas = ganadas + perdidas;

        BigDecimal tasaConversion = BigDecimal.ZERO;
        if (totalCerradas > 0) {
            tasaConversion = BigDecimal.valueOf(ganadas)
                    .divide(BigDecimal.valueOf(totalCerradas), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // 5. Oportunidades que cierran este mes (próximos 30 días)
        LocalDate limiteCierre = hoy.plusDays(30);
        long opsCierreMes = activas.stream()
                .filter(o -> o.getFechaEstimadaCierre() != null 
                        && !o.getFechaEstimadaCierre().isBefore(hoy) 
                        && !o.getFechaEstimadaCierre().isAfter(limiteCierre))
                .count();

        // 6. Seguimientos Vencidos (estado = PENDIENTE y fecha_programada < hoy)
        List<Seguimiento> seguimientosVencidosList;
        if (esEjecutivo) {
            seguimientosVencidosList = seguimientoRepository.findByEjecutivoIdAndEstadoAndFechaProgramadaBefore(
                    ejecutivoId, EstadoSeguimiento.PENDIENTE, hoy);
        } else {
            seguimientosVencidosList = seguimientoRepository.findByEstadoAndFechaProgramadaBefore(
                    EstadoSeguimiento.PENDIENTE, hoy);
        }
        long segVencidos = seguimientosVencidosList.size();

        // 7. Visitas últimos 7 días por ejecutivo
        LocalDate hace7Dias = hoy.minusDays(7);
        List<Visita> visitasRecientes;
        if (esEjecutivo) {
            visitasRecientes = visitaRepository.findByEjecutivoIdOrderByFechaDesc(ejecutivoId).stream()
                    .filter(v -> !v.getFecha().isBefore(hace7Dias))
                    .toList();
        } else {
            visitasRecientes = visitaRepository.findAllByOrderByFechaDesc().stream()
                    .filter(v -> !v.getFecha().isBefore(hace7Dias))
                    .toList();
        }

        Map<Long, Long> visitasPorEjecutivo = visitasRecientes.stream()
                .collect(Collectors.groupingBy(Visita::getEjecutivoId, Collectors.counting()));

        List<DashboardResponse.ExecutiveVisitCount> visitasKpi = new ArrayList<>();
        visitasPorEjecutivo.forEach((id, count) -> {
            String nombre = usuarioRepository.findById(id)
                    .map(u -> u.getNombre() + " " + u.getApellido())
                    .orElse("Ejecutivo " + id);
            visitasKpi.add(DashboardResponse.ExecutiveVisitCount.builder()
                    .ejecutivoId(id)
                    .ejecutivoNombre(nombre)
                    .cantidadVisitas(count)
                    .build());
        });
        visitasKpi.sort(Comparator.comparing(DashboardResponse.ExecutiveVisitCount::getCantidadVisitas).reversed());

        // 8. Cumplimiento mensual
        int anioActual = hoy.getYear();
        int mesActual = hoy.getMonthValue();
        List<Venta> ventasPeriodo;
        if (esEjecutivo) {
            ventasPeriodo = ventaRepository.findByEjecutivoIdOrderByAnioDescMesDesc(ejecutivoId).stream()
                    .filter(v -> v.getAnio() == anioActual && v.getMes() == mesActual)
                    .toList();
        } else {
            ventasPeriodo = ventaRepository.findByAnioAndMes(anioActual, mesActual);
        }

        List<DashboardResponse.ExecutiveSalesPerformance> rendimientoKpi = ventasPeriodo.stream().map(v -> {
            String nombre = usuarioRepository.findById(v.getEjecutivoId())
                    .map(u -> u.getNombre() + " " + u.getApellido())
                    .orElse("Ejecutivo " + v.getEjecutivoId());

            BigDecimal porcentaje = BigDecimal.ZERO;
            if (v.getMeta().compareTo(BigDecimal.ZERO) > 0) {
                porcentaje = v.getVentaReal()
                        .divide(v.getMeta(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            return DashboardResponse.ExecutiveSalesPerformance.builder()
                    .ejecutivoId(v.getEjecutivoId())
                    .ejecutivoNombre(nombre)
                    .meta(v.getMeta())
                    .ventaReal(v.getVentaReal())
                    .forecast(v.getForecast())
                    .porcentajeCumplimiento(porcentaje)
                    .build();
        }).collect(Collectors.toList());

        // 9. Alertas de inactividad (> 24 horas sin registrar login/actividad)
        List<DashboardResponse.InactiveExecutive> alertasInactividad = new ArrayList<>();
        if (!esEjecutivo) {
            LocalDateTime limiteInactividad = LocalDateTime.now().minusHours(24);
            List<Usuario> ejecutivos = usuarioRepository.findAll().stream()
                    .filter(u -> u.getActivo() && u.getRol() == RolUsuario.EJECUTIVO)
                    .toList();

            for (Usuario ej : ejecutivos) {
                if (ej.getUltimaActividad() == null || ej.getUltimaActividad().isBefore(limiteInactividad)) {
                    String ultAct = ej.getUltimaActividad() != null 
                            ? ej.getUltimaActividad().toString() 
                            : "Nunca";
                    alertasInactividad.add(DashboardResponse.InactiveExecutive.builder()
                            .ejecutivoId(ej.getId())
                            .ejecutivoNombre(ej.getNombre() + " " + ej.getApellido())
                            .ultimaActividad(ultAct)
                            .build());
                }
            }
        }

        return DashboardResponse.builder()
                .valorTotalPipeline(valorTotal)
                .valorPonderadoPipeline(valorPonderado)
                .tasaConversion(tasaConversion)
                .oportunidadesCierreMes(opsCierreMes)
                .seguimientosVencidos(segVencidos)
                .visitasUltimos7Dias(visitasKpi)
                .cumplimientoMensual(rendimientoKpi)
                .alertasInactividad(alertasInactividad)
                .build();
    }
}
