package com.reactivosdelvalle.crm_api.audit;

import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Escucha los eventos de base de datos de Hibernate y registra en la tabla
 * auditoria cada INSERT y cada cambio de campo (UPDATE) de las entidades del CRM.
 *
 * Las filas se escriben con JdbcTemplate para unirse a la misma transacción
 * sin pasar por la sesión de Hibernate (evita recursión del listener).
 */
@Component
public class AuditEventListener implements PostInsertEventListener, PreUpdateEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private static final Set<String> TABLAS_AUDITADAS = new HashSet<>(Arrays.asList(
            "clientes", "prospectos", "oportunidades", "visitas",
            "seguimientos", "ventas", "usuarios", "contactos", "catalogos"));

    /** Propiedades técnicas que no se auditan a nivel de campo. */
    private static final Set<String> PROPIEDAD_IGNORADAS = new HashSet<>(Arrays.asList(
            "createdAt", "updatedAt", "updatedById", "valorPonderado", "ultimaActividad",
            "alertaInactividad"));

    private final JdbcTemplate jdbcTemplate;

    public AuditEventListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        String tabla = nombreTabla(event.getPersister().getTableName());
        log.debug("PRE_UPDATE detectado en tabla {}", tabla);
        if (!TABLAS_AUDITADAS.contains(tabla)) {
            return false;
        }

        Object[] estadoAnterior = event.getOldState();
        Object[] estadoNuevo = event.getState();
        String[] propiedades = event.getPersister().getPropertyNames();

        if (estadoAnterior == null) {
            return false;
        }

        Long usuarioId = obtenerUsuarioId();
        String ip = obtenerIpAddress();

        for (int i = 0; i < propiedades.length; i++) {
            String propiedad = propiedades[i];
            if (PROPIEDAD_IGNORADAS.contains(propiedad)) {
                continue;
            }

            Object anterior = estadoAnterior[i];
            Object nuevo = estadoNuevo[i];

            if (anterior == null && nuevo == null) {
                continue;
            }
            if (anterior != null && anterior.equals(nuevo)) {
                continue;
            }

            // Borrado lógico: activo pasa de true a false
            if ("activo".equals(propiedad) && Boolean.TRUE.equals(anterior) && Boolean.FALSE.equals(nuevo)) {
                insertarAuditoria(tabla, (Long) event.getId(), usuarioId, "DELETE_LOGICO",
                        propiedad, aTexto(anterior), aTexto(nuevo), ip);
            } else {
                insertarAuditoria(tabla, (Long) event.getId(), usuarioId, "UPDATE",
                        propiedad, aTexto(anterior), aTexto(nuevo), ip);
            }
        }

        return false;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        String tabla = nombreTabla(event.getPersister().getTableName());
        log.debug("POST_INSERT detectado en tabla {} id {}", tabla, event.getId());
        if (!TABLAS_AUDITADAS.contains(tabla)) {
            return;
        }

        insertarAuditoria(tabla, (Long) event.getId(), obtenerUsuarioId(), "INSERT",
                null, null, null, obtenerIpAddress());
    }

    private void insertarAuditoria(String tabla, Long registroId, Long usuarioId, String accion,
                                   String campo, String valorAnterior, String valorNuevo, String ip) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO auditoria (tabla_nombre, registro_id, usuario_id, accion, " +
                            "campo_modificado, valor_anterior, valor_nuevo, ip_address) " +
                            "VALUES (?, ?, ?, CAST(? AS accion_auditoria), ?, ?, ?, ?)",
                    tabla, registroId, usuarioId, accion, campo, valorAnterior, valorNuevo, ip);
        } catch (Exception ex) {
            // La auditoría nunca debe interrumpir la operación de negocio
            log.error("No se pudo registrar auditoria para {}#{}: {}", tabla, registroId, ex.getMessage(), ex);
        }
    }

    private Long obtenerUsuarioId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (auth.getPrincipal() instanceof UsuarioPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private String obtenerIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    private String nombreTabla(String nombreCompleto) {
        int idx = nombreCompleto.lastIndexOf('.');
        return idx >= 0 ? nombreCompleto.substring(idx + 1) : nombreCompleto;
    }

    private String aTexto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}
