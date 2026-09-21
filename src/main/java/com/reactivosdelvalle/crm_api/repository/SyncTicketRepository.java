package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.SyncTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyncTicketRepository extends JpaRepository<SyncTicket, Long> {

    Optional<SyncTicket> findByClienteId(Long clienteId);

    List<SyncTicket> findByEstadoIn(List<String> estados);
}
