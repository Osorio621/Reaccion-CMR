package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.TokenReseteo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenReseteoRepository extends JpaRepository<TokenReseteo, Long> {

    Optional<TokenReseteo> findByTokenHashAndUsadoFalse(String tokenHash);

    void deleteByUsuarioId(Long usuarioId);

    void deleteByExpiraEnBefore(LocalDateTime limite);
}
