package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByEjecutivoIdAndAnioAndMes(Long ejecutivoId, Integer anio, Integer mes);
    List<Venta> findByEjecutivoIdOrderByAnioDescMesDesc(Long ejecutivoId);
    List<Venta> findByAnioAndMes(Integer anio, Integer mes);
    List<Venta> findAllByOrderByAnioDescMesDesc();

    @Query(value = "SELECT " +
            "CASE WHEN v.mes < 10 THEN v.anio || '-0' || v.mes ELSE v.anio || '-' || v.mes END, " +
            "u.nombre || ' ' || u.apellido, " +
            "v.meta, v.venta_real, v.forecast " +
            "FROM ventas v " +
            "JOIN usuarios u ON v.ejecutivo_id = u.id " +
            "WHERE v.ejecutivo_id = :ejecutivoId " +
            "AND (v.anio * 100 + v.mes) BETWEEN " +
            "    (EXTRACT(YEAR FROM CAST(:desde AS date))::int * 100 + EXTRACT(MONTH FROM CAST(:desde AS date))::int) " +
            "    AND (EXTRACT(YEAR FROM CAST(:hasta AS date))::int * 100 + EXTRACT(MONTH FROM CAST(:hasta AS date))::int) " +
            "ORDER BY v.anio, v.mes", nativeQuery = true)
    List<Object[]> findByEjecutivoIdAndPeriodo(
            @Param("ejecutivoId") Long ejecutivoId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT " +
            "CASE WHEN v.mes < 10 THEN v.anio || '-0' || v.mes ELSE v.anio || '-' || v.mes END, " +
            "u.nombre || ' ' || u.apellido, " +
            "v.meta, v.venta_real, v.forecast " +
            "FROM ventas v " +
            "JOIN usuarios u ON v.ejecutivo_id = u.id " +
            "WHERE (v.anio * 100 + v.mes) BETWEEN " +
            "    (EXTRACT(YEAR FROM CAST(:desde AS date))::int * 100 + EXTRACT(MONTH FROM CAST(:desde AS date))::int) " +
            "    AND (EXTRACT(YEAR FROM CAST(:hasta AS date))::int * 100 + EXTRACT(MONTH FROM CAST(:hasta AS date))::int) " +
            "ORDER BY v.anio, v.mes", nativeQuery = true)
    List<Object[]> findByPeriodo(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
