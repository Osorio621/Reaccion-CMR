package com.reactivosdelvalle.crm_api.repository;

import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailAndActivoTrue(String email);

    @Query("SELECT u FROM Usuario u WHERE u.activo = true AND u.rol = :rol " +
            "AND (u.ultimaActividad IS NULL OR u.ultimaActividad < :limite)")
    List<Usuario> findActivosSinActividadDesde(@Param("rol") RolUsuario rol,
                                               @Param("limite") LocalDateTime limite);
}
