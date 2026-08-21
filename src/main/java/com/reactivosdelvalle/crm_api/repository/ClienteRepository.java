package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByEjecutivoIdAndActivoTrueOrderByNombreAsc(Long ejecutivoId);

    List<Cliente> findAllByActivoTrueOrderByNombreAsc();

    Optional<Cliente> findByIdAndActivoTrue(Long id);
}