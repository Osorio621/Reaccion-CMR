package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Comunicacion;
import com.reactivosdelvalle.crm_api.entity.TipoEntidadComunicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComunicacionRepository extends JpaRepository<Comunicacion, Long> {

    List<Comunicacion> findByEntidadTipoAndEntidadIdOrderByFechaDesc(
            TipoEntidadComunicacion entidadTipo, Long entidadId);
}
