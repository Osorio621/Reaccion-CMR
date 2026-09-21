package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.BaseIntegrationTest;
import com.reactivosdelvalle.crm_api.entity.EstadoSeguimiento;
import com.reactivosdelvalle.crm_api.entity.Seguimiento;
import com.reactivosdelvalle.crm_api.repository.SeguimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para marcarVencidos() con BD real (Testcontainers + Flyway).
 * Cada test corre en su propia transacción que se revierte al terminar (@Transactional),
 * así no necesitamos @AfterEach para limpiar.
 */
@Transactional
class SeguimientoVencimientoTest extends BaseIntegrationTest {

    @Autowired
    private SeguimientoRepository seguimientoRepository;

    @Autowired
    private SeguimientoService seguimientoService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long ejecutivoId;
    private Long oportunidadId;

    @BeforeEach
    void setUp() {
        // El seed V13 creó el admin; obtenemos su ID real
        ejecutivoId = jdbc.queryForObject(
                "SELECT id FROM usuarios WHERE email = 'admin@reactivosdelvalle.com'", Long.class);

        // Catálogo de etapa para la oportunidad
        Long etapaId = jdbc.queryForObject(
                "SELECT id FROM catalogos WHERE codigo = 'ETAPA_PROSPECCION'", Long.class);

        // Cliente mínimo (solo requiere nombre + ejecutivo_id)
        Long clienteId = jdbc.queryForObject(
                "INSERT INTO clientes (nombre, ejecutivo_id, activo, created_at, updated_at) " +
                "VALUES ('Test Cliente IT', ?, true, now(), now()) RETURNING id",
                Long.class, ejecutivoId);

        // Oportunidad mínima requerida por FK de seguimientos
        oportunidadId = jdbc.queryForObject(
                "INSERT INTO oportunidades " +
                "(nombre, cliente_id, ejecutivo_id, etapa_id, valor, probabilidad, " +
                " fecha_estimada_cierre, proxima_accion, fecha_proxima_accion, activo, created_at, updated_at) " +
                "VALUES ('Test Op IT', ?, ?, ?, 1000.00, 50, " +
                "        current_date + 30, 'accion test', current_date + 7, true, now(), now()) RETURNING id",
                Long.class, clienteId, ejecutivoId, etapaId);
    }

    @Test
    @DisplayName("PENDIENTE vencido → VENCIDO; PENDIENTE futuro y COMPLETADO no cambian")
    void marcarVencidos_cambiaSoloPendientesConFechaPasada() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        LocalDate manana = LocalDate.now().plusDays(1);

        Seguimiento pendienteVencido = seguimientoRepository.save(nuevoSeg(ayer, EstadoSeguimiento.PENDIENTE));
        Seguimiento pendienteFuturo  = seguimientoRepository.save(nuevoSeg(manana, EstadoSeguimiento.PENDIENTE));
        Seguimiento completadoViejo  = seguimientoRepository.save(nuevoSeg(ayer, EstadoSeguimiento.COMPLETADO));

        int actualizados = seguimientoService.marcarVencidos();

        assertThat(actualizados).isEqualTo(1);
        assertThat(seguimientoRepository.findById(pendienteVencido.getId()))
                .get().extracting(Seguimiento::getEstado)
                .isEqualTo(EstadoSeguimiento.VENCIDO);
        assertThat(seguimientoRepository.findById(pendienteFuturo.getId()))
                .get().extracting(Seguimiento::getEstado)
                .isEqualTo(EstadoSeguimiento.PENDIENTE);
        assertThat(seguimientoRepository.findById(completadoViejo.getId()))
                .get().extracting(Seguimiento::getEstado)
                .isEqualTo(EstadoSeguimiento.COMPLETADO);
    }

    @Test
    @DisplayName("Sin seguimientos vencidos retorna 0")
    void marcarVencidos_sinVencidos_retornaCero() {
        seguimientoRepository.save(nuevoSeg(LocalDate.now().plusDays(3), EstadoSeguimiento.PENDIENTE));

        assertThat(seguimientoService.marcarVencidos()).isZero();
    }

    @Test
    @DisplayName("marcarVencidos actualiza updatedAt en los registros modificados")
    void marcarVencidos_actualizaUpdatedAt() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        Seguimiento guardado = seguimientoRepository.save(nuevoSeg(LocalDate.now().minusDays(2), EstadoSeguimiento.PENDIENTE));

        seguimientoService.marcarVencidos();

        assertThat(seguimientoRepository.findById(guardado.getId()))
                .get().extracting(Seguimiento::getUpdatedAt)
                .satisfies(ts -> assertThat((LocalDateTime) ts).isAfter(antes));
    }

    private Seguimiento nuevoSeg(LocalDate fechaProgramada, EstadoSeguimiento estado) {
        return Seguimiento.builder()
                .oportunidadId(oportunidadId)
                .ejecutivoId(ejecutivoId)
                .tipo("SEGUIMIENTO_LLAMADA")
                .fechaProgramada(fechaProgramada)
                .estado(estado)
                .notas("test IT")
                .build();
    }
}
