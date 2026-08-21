package com.reactivosdelvalle.crm_api.util;

import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Obtiene el usuario autenticado del contexto de Spring Security.
     */
    public UsuarioPrincipal getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof UsuarioPrincipal) {
            return (UsuarioPrincipal) principal;
        }
        return null;
    }

    /**
     * Verifica si el usuario actual tiene el rol de ADMIN.
     */
    public boolean esAdmin() {
        UsuarioPrincipal usuario = getUsuarioActual();
        return usuario != null && usuario.getRol() == RolUsuario.ADMIN;
    }

    /**
     * Verifica si el usuario actual es GERENTE o ADMIN.
     */
    public boolean esGerenteOAdmin() {
        UsuarioPrincipal usuario = getUsuarioActual();
        if (usuario == null) return false;
        RolUsuario rol = usuario.getRol();
        return rol == RolUsuario.GERENTE || rol == RolUsuario.ADMIN;
    }

    /**
     * Verifica si el usuario actual es el propietario de los registros (coincide con su ejecutivo_id).
     */
    public boolean esPropietario(Long ejecutivoId) {
        UsuarioPrincipal usuario = getUsuarioActual();
        return usuario != null && usuario.getId().equals(ejecutivoId);
    }

    /**
     * Determina si el usuario actual puede acceder a registros de un ejecutivo dado.
     * Permitido si es ADMIN, GERENTE, o si es el propietario.
     */
    public boolean puedeAccederA(Long ejecutivoId) {
        return esGerenteOAdmin() || esPropietario(ejecutivoId);
    }
}
