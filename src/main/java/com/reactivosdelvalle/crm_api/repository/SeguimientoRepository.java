package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.EstadoSeguimiento;
import com.reactivosdelvalle.crm_api.entity.Seguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {
    List<Seguimiento> findByEjecutivoIdOrderByFechaProgramadaDesc(Long ejecutivoId);
    List<Seguimiento> findAllByOrderByFechaProgramadaDesc();
    List<Seguimiento> findByOportunidadIdOrderByFechaProgramadaDesc(Long oportunidadId);
    List<Seguimiento> findByEjecutivoIdAndEstadoAndFechaProgramadaBefore(Long ejecutivoId, EstadoSeguimiento estado, LocalDate fecha);
    List<Seguimiento> findByEstadoAndFechaProgramadaBefore(EstadoSeguimiento estado, LocalDate fecha);
}
