package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Prospecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProspectoRepository extends JpaRepository<Prospecto, Long> {

    List<Prospecto> findByResponsableIdAndActivoTrueOrderByFechaProximaAccionAsc(Long responsableId);

    List<Prospecto> findAllByActivoTrueOrderByFechaProximaAccionAsc();

    Optional<Prospecto> findByIdAndActivoTrue(Long id);
}