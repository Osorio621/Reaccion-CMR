package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Archivo;
import com.reactivosdelvalle.crm_api.entity.TipoEntidadArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchivoRepository extends JpaRepository<Archivo, Long> {
    List<Archivo> findByEntidadTipoAndEntidadIdOrderByCreatedAtDesc(
            TipoEntidadArchivo entidadTipo, Long entidadId);
}
