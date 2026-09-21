package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByEjecutivoIdAndActivoTrueOrderByNombreAsc(Long ejecutivoId);

    List<Cliente> findAllByActivoTrueOrderByNombreAsc();

    Optional<Cliente> findByIdAndActivoTrue(Long id);

    @Query(value = "SELECT c.nombre, c.razon_social, c.email_principal, c.telefono_principal, " +
            "u.nombre || ' ' || u.apellido, " +
            "cat_tipo.nombre, cat_zona.nombre " +
            "FROM clientes c " +
            "LEFT JOIN usuarios u ON c.ejecutivo_id = u.id " +
            "LEFT JOIN catalogos cat_tipo ON c.tipo_id = cat_tipo.id " +
            "LEFT JOIN catalogos cat_zona ON c.zona_id = cat_zona.id " +
            "WHERE c.activo = true " +
            "AND c.ejecutivo_id = :ejecutivoId " +
            "AND c.created_at BETWEEN :desde AND :hasta " +
            "ORDER BY c.nombre", nativeQuery = true)
    List<Object[]> findExportByEjecutivoIdAndFecha(
            @Param("ejecutivoId") Long ejecutivoId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT c.nombre, c.razon_social, c.email_principal, c.telefono_principal, " +
            "u.nombre || ' ' || u.apellido, " +
            "cat_tipo.nombre, cat_zona.nombre " +
            "FROM clientes c " +
            "LEFT JOIN usuarios u ON c.ejecutivo_id = u.id " +
            "LEFT JOIN catalogos cat_tipo ON c.tipo_id = cat_tipo.id " +
            "LEFT JOIN catalogos cat_zona ON c.zona_id = cat_zona.id " +
            "WHERE c.activo = true " +
            "AND c.created_at BETWEEN :desde AND :hasta " +
            "ORDER BY c.nombre", nativeQuery = true)
    List<Object[]> findExportByFecha(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
