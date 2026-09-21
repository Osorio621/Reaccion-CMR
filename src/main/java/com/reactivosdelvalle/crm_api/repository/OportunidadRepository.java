package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.service.OportunidadExportRow;
import com.reactivosdelvalle.crm_api.service.PipelineExportRow;
import com.reactivosdelvalle.crm_api.entity.Oportunidad;
import com.reactivosdelvalle.crm_api.entity.EstadoOportunidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OportunidadRepository extends JpaRepository<Oportunidad, Long> {

    List<Oportunidad> findByEjecutivoIdAndActivoTrueAndEstadoOrderByUpdatedAtDesc(
            Long ejecutivoId, EstadoOportunidad estado);

    List<Oportunidad> findAllByActivoTrueAndEstadoOrderByUpdatedAtDesc(
            EstadoOportunidad estado);

    List<Oportunidad> findByEjecutivoIdAndActivoTrueOrderByUpdatedAtDesc(Long ejecutivoId);

    List<Oportunidad> findAllByActivoTrueOrderByUpdatedAtDesc();

    Optional<Oportunidad> findByIdAndActivoTrue(Long id);

    @Query(value = "SELECT o.nombre, cli.nombre, cat.nombre, o.valor, o.probabilidad, " +
            "o.estado::text, o.fecha_cierre_real " +
            "FROM oportunidades o " +
            "JOIN clientes cli ON o.cliente_id = cli.id " +
            "JOIN catalogos cat ON o.etapa_id = cat.id " +
            "WHERE o.activo = true " +
            "AND o.ejecutivo_id = :ejecutivoId " +
            "AND o.estado::text = :estado " +
            "AND o.created_at BETWEEN :desde AND :hasta " +
            "ORDER BY o.created_at DESC", nativeQuery = true)
    List<Object[]> findExportByEjecutivoIdAndEstadoAndFecha(
            @Param("ejecutivoId") Long ejecutivoId,
            @Param("estado") String estado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT o.nombre, cli.nombre, cat.nombre, o.valor, o.probabilidad, " +
            "o.estado::text, o.fecha_cierre_real " +
            "FROM oportunidades o " +
            "JOIN clientes cli ON o.cliente_id = cli.id " +
            "JOIN catalogos cat ON o.etapa_id = cat.id " +
            "WHERE o.activo = true " +
            "AND o.ejecutivo_id = :ejecutivoId " +
            "AND o.created_at BETWEEN :desde AND :hasta " +
            "ORDER BY o.created_at DESC", nativeQuery = true)
    List<Object[]> findExportByEjecutivoIdAndFecha(
            @Param("ejecutivoId") Long ejecutivoId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT o.nombre, cli.nombre, cat.nombre, o.valor, o.probabilidad, " +
            "o.estado::text, o.fecha_cierre_real " +
            "FROM oportunidades o " +
            "JOIN clientes cli ON o.cliente_id = cli.id " +
            "JOIN catalogos cat ON o.etapa_id = cat.id " +
            "WHERE o.activo = true " +
            "AND o.estado::text = :estado " +
            "AND o.created_at BETWEEN :desde AND :hasta " +
            "ORDER BY o.created_at DESC", nativeQuery = true)
    List<Object[]> findExportByEstadoAndFecha(
            @Param("estado") String estado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT o.nombre, cli.nombre, cat.nombre, o.valor, o.probabilidad, " +
            "o.estado::text, o.fecha_cierre_real " +
            "FROM oportunidades o " +
            "JOIN clientes cli ON o.cliente_id = cli.id " +
            "JOIN catalogos cat ON o.etapa_id = cat.id " +
            "WHERE o.activo = true " +
            "AND o.created_at BETWEEN :desde AND :hasta " +
            "ORDER BY o.created_at DESC", nativeQuery = true)
    List<Object[]> findExportByFecha(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT cat.nombre, COUNT(o), SUM(o.valor), SUM(o.valor * o.probabilidad / 100) " +
            "FROM oportunidades o " +
            "JOIN catalogos cat ON o.etapa_id = cat.id " +
            "WHERE o.activo = true " +
            "AND o.estado::text = 'ACTIVA' " +
            "AND o.ejecutivo_id = :ejecutivoId " +
            "GROUP BY cat.nombre, cat.orden " +
            "ORDER BY cat.orden", nativeQuery = true)
    List<Object[]> getPipelineByEjecutivo(@Param("ejecutivoId") Long ejecutivoId);

    @Query(value = "SELECT cat.nombre, COUNT(o), SUM(o.valor), SUM(o.valor * o.probabilidad / 100) " +
            "FROM oportunidades o " +
            "JOIN catalogos cat ON o.etapa_id = cat.id " +
            "WHERE o.activo = true " +
            "AND o.estado::text = 'ACTIVA' " +
            "GROUP BY cat.nombre, cat.orden " +
            "ORDER BY cat.orden", nativeQuery = true)
    List<Object[]> getPipelineGlobal();
}
