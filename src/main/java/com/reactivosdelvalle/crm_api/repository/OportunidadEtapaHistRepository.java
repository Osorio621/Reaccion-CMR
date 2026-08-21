package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.OportunidadEtapaHist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OportunidadEtapaHistRepository extends JpaRepository<OportunidadEtapaHist, Long> {

    List<OportunidadEtapaHist> findByOportunidadIdOrderByCreatedAtDesc(Long oportunidadId);
}