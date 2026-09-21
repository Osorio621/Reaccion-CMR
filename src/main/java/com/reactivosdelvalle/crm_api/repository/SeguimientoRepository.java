package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.EstadoSeguimiento;
import com.reactivosdelvalle.crm_api.entity.Seguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {
    List<Seguimiento> findByEjecutivoIdOrderByFechaProgramadaDesc(Long ejecutivoId);
    List<Seguimiento> findAllByOrderByFechaProgramadaDesc();
    List<Seguimiento> findByOportunidadIdOrderByFechaProgramadaDesc(Long oportunidadId);
    List<Seguimiento> findByEjecutivoIdAndEstadoAndFechaProgramadaBefore(Long ejecutivoId, EstadoSeguimiento estado, LocalDate fecha);
    List<Seguimiento> findByEstadoAndFechaProgramadaBefore(EstadoSeguimiento estado, LocalDate fecha);

    /**
     * Bulk update: PENDIENTE → VENCIDO para todos los que pasaron su fecha_programada.
     * Bypasa @PreUpdate (comportamiento normal en JPQL bulk updates), por eso
     * se actualiza updatedAt explícitamente en la query.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Seguimiento s SET s.estado = :nuevo, s.updatedAt = :ahora " +
           "WHERE s.estado = :actual AND s.fechaProgramada < :hoy")
    int marcarVencidos(@Param("actual") EstadoSeguimiento actual,
                       @Param("nuevo")  EstadoSeguimiento nuevo,
                       @Param("hoy")    LocalDate hoy,
                       @Param("ahora")  LocalDateTime ahora);

    /** Vencidos para un ejecutivo: estado VENCIDO, o PENDIENTE con fecha ya pasada. */
    @Query("SELECT s FROM Seguimiento s WHERE s.ejecutivoId = :eid " +
           "AND (s.estado = 'VENCIDO' OR (s.estado = 'PENDIENTE' AND s.fechaProgramada < :hoy)) " +
           "ORDER BY s.fechaProgramada DESC")
    List<Seguimiento> findVencidosByEjecutivo(@Param("eid") Long ejecutivoId,
                                              @Param("hoy") LocalDate hoy);

    /** Pendientes para mañana: base del recordatorio preventivo (día antes). */
    @Query("SELECT s FROM Seguimiento s WHERE s.estado = 'PENDIENTE' AND s.fechaProgramada = :manana")
    List<Seguimiento> findPendientesParaRecordatorio(@Param("manana") LocalDate manana);

    /** Vencidos globales: estado VENCIDO, o PENDIENTE con fecha ya pasada. */
    @Query("SELECT s FROM Seguimiento s " +
           "WHERE s.estado = 'VENCIDO' OR (s.estado = 'PENDIENTE' AND s.fechaProgramada < :hoy) " +
           "ORDER BY s.fechaProgramada DESC")
    List<Seguimiento> findVencidos(@Param("hoy") LocalDate hoy);
}
