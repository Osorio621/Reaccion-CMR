package com.reactivosdelvalle.crm_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bloquea todos los endpoints excepto /api/auth/** cuando el usuario autenticado
 * tiene requiere_cambio_password = true.
 * Esto garantiza que la cuenta admin del seed no opere con la contraseña pública
 * del repositorio en una instalación productiva.
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.getPrincipal() instanceof UsuarioPrincipal principal
                && principal.isRequiereCambioPassword()) {

            String path = request.getRequestURI();
            if (!path.startsWith("/api/auth/")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"error\":\"REQUIERE_CAMBIO_PASSWORD\"," +
                        "\"mensaje\":\"Debes cambiar tu contraseña antes de continuar.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
