package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Prospecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProspectoRepository extends JpaRepository<Prospecto, Long> {

    List<Prospecto> findByResponsableIdAndActivoTrueOrderByFechaProximaAccionAsc(Long responsableId);

    List<Prospecto> findAllByActivoTrueOrderByFechaProximaAccionAsc();

    Optional<Prospecto> findByIdAndActivoTrue(Long id);

    @Query(value = "SELECT p.nombre, COALESCE(p.empresa, ''), p.email, " +
            "cat.nombre, " +
            "COALESCE(p.sitio_web, ''), p.created_at " +
            "FROM prospectos p " +
            "LEFT JOIN catalogos cat ON p.tipo_id = cat.id " +
            "WHERE p.activo = true " +
            "AND p.responsable_id = :responsableId " +
            "AND p.created_at BETWEEN :desde AND :hasta " +
            "ORDER BY p.created_at DESC", nativeQuery = true)
    List<Object[]> findExportByResponsableIdAndFecha(
            @Param("responsableId") Long responsableId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT p.nombre, COALESCE(p.empresa, ''), p.email, " +
            "cat.nombre, " +
            "COALESCE(p.sitio_web, ''), p.created_at " +
            "FROM prospectos p " +
            "LEFT JOIN catalogos cat ON p.tipo_id = cat.id " +
            "WHERE p.activo = true " +
            "AND p.created_at BETWEEN :desde AND :hasta " +
            "ORDER BY p.created_at DESC", nativeQuery = true)
    List<Object[]> findExportByFecha(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
