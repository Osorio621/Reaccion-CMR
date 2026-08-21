package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByEjecutivoIdAndAnioAndMes(Long ejecutivoId, Integer anio, Integer mes);
    List<Venta> findByEjecutivoIdOrderByAnioDescMesDesc(Long ejecutivoId);
    List<Venta> findByAnioAndMes(Integer anio, Integer mes);
    List<Venta> findAllByOrderByAnioDescMesDesc();
}
