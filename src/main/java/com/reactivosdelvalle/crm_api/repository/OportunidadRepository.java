package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Oportunidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OportunidadRepository extends JpaRepository<Oportunidad, Long> {

    List<Oportunidad> findByEjecutivoIdAndActivoTrueAndEstadoOrderByUpdatedAtDesc(
            Long ejecutivoId, com.reactivosdelvalle.crm_api.entity.EstadoOportunidad estado);

    List<Oportunidad> findAllByActivoTrueAndEstadoOrderByUpdatedAtDesc(
            com.reactivosdelvalle.crm_api.entity.EstadoOportunidad estado);

    List<Oportunidad> findByEjecutivoIdAndActivoTrueOrderByUpdatedAtDesc(Long ejecutivoId);

    List<Oportunidad> findAllByActivoTrueOrderByUpdatedAtDesc();

    Optional<Oportunidad> findByIdAndActivoTrue(Long id);
}