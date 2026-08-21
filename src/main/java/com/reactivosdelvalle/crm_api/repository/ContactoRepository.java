package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactoRepository extends JpaRepository<Contacto, Long> {

    List<Contacto> findByClienteIdAndActivoTrueOrderByNombreAsc(Long clienteId);

    Optional<Contacto> findByIdAndActivoTrue(Long id);

    @Modifying
    @Query("UPDATE Contacto c SET c.esPrincipal = false WHERE c.clienteId = :clienteId AND c.esPrincipal = true AND c.activo = true")
    void quitarPrincipal(@Param("clienteId") Long clienteId);
}