package com.reactivosdelvalle.crm_api.config;

import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Logging profesional de acceso:
 * - Asigna un ID de correlación (reqId) a cada petición y lo propaga en el
 *   header X-Request-Id y en el MDC, para rastrear una operación en el log.
 * - Emite UNA línea por petición: método, ruta, estado HTTP, duración,
 *   usuario autenticado e IP. Nunca registra cuerpos ni credenciales.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String MDC_REQ_ID = "reqId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        String reqId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_REQ_ID, reqId);
        response.setHeader("X-Request-Id", reqId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            registrarAcceso(request, response, System.currentTimeMillis() - inicio);
            MDC.remove(MDC_REQ_ID);
        }
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        // El forward a /error ya quedó registrado en la línea de la petición original
        return true;
    }

    private void registrarAcceso(HttpServletRequest request, HttpServletResponse response, long duracionMs) {
        String uri = request.getRequestURI();
        if ("/error".equals(uri)) {
            return;
        }

        String mensaje = "{} {} {} {}ms usuario={} ip={}";
        Object[] argumentos = {
                request.getMethod(),
                uri + (request.getQueryString() != null ? "?" + request.getQueryString() : ""),
                response.getStatus(),
                duracionMs,
                usuarioAutenticado(),
                ipCliente(request)
        };

        int estado = response.getStatus();
        if (estado >= 500) {
            log.error(mensaje, argumentos);
        } else if (estado >= 400) {
            log.warn(mensaje, argumentos);
        } else {
            log.info(mensaje, argumentos);
        }
    }

    private String usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getPrincipal() instanceof UsuarioPrincipal principal) {
            return principal.getUsername();
        }
        return "-";
    }

    private String ipCliente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
