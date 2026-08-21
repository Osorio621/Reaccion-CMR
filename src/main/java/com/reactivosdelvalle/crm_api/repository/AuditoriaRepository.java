package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.Auditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    List<Auditoria> findByTablaNombreAndRegistroIdOrderByCreatedAtDescIdDesc(String tablaNombre, Long registroId);

    List<Auditoria> findByUsuarioIdOrderByCreatedAtDescIdDesc(Long usuarioId, Pageable pageable);
}
