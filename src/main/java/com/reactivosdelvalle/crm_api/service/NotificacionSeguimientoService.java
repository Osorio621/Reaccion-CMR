package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.entity.Seguimiento;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.repository.OportunidadRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orquesta las notificaciones de seguimientos por email y WhatsApp.
 *
 * Cada método es tolerante a fallos: un ejecutivo sin teléfono o email
 * configurado solo produce un warning en el log, sin romper el lote.
 */
@Service
public class NotificacionSeguimientoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionSeguimientoService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final UsuarioRepository usuarioRepository;
    private final OportunidadRepository oportunidadRepository;

    public NotificacionSeguimientoService(EmailService emailService,
                                          WhatsAppService whatsAppService,
                                          UsuarioRepository usuarioRepository,
                                          OportunidadRepository oportunidadRepository) {
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.usuarioRepository = usuarioRepository;
        this.oportunidadRepository = oportunidadRepository;
    }

    public void notificarRecordatorios(List<Seguimiento> seguimientos) {
        for (Seguimiento seg : seguimientos) {
            try {
                notificarRecordatorio(seg);
            } catch (Exception ex) {
                log.error("Error al notificar recordatorio del seguimiento {}: {}", seg.getId(), ex.getMessage());
            }
        }
    }

    public void notificarVencidos(List<Seguimiento> seguimientos) {
        for (Seguimiento seg : seguimientos) {
            try {
                notificarVencido(seg);
            } catch (Exception ex) {
                log.error("Error al notificar vencimiento del seguimiento {}: {}", seg.getId(), ex.getMessage());
            }
        }
    }

    private void notificarRecordatorio(Seguimiento seg) {
        Usuario ejecutivo = resolverEjecutivo(seg.getEjecutivoId());
        if (ejecutivo == null) return;

        String nombreOp = resolverNombreOportunidad(seg.getOportunidadId());
        String fecha = seg.getFechaProgramada().format(FMT);
        String nombreCompleto = ejecutivo.getNombre() + " " + ejecutivo.getApellido();

        emailService.enviarRecordatorio(
                ejecutivo.getEmail(), nombreCompleto,
                seg.getTipo(), nombreOp, fecha);

        String mensajeWa = String.format(
                "📋 *Recordatorio CRM*\n\nHola %s, mañana tenés un seguimiento pendiente:\n" +
                "• Oportunidad: %s\n• Tipo: %s\n• Fecha: %s\n\nIngresá al CRM para ver los detalles.",
                ejecutivo.getNombre(), nombreOp, seg.getTipo(), fecha);

        whatsAppService.enviar(ejecutivo.getTelefono(), mensajeWa);
    }

    private void notificarVencido(Seguimiento seg) {
        Usuario ejecutivo = resolverEjecutivo(seg.getEjecutivoId());
        if (ejecutivo == null) return;

        String nombreOp = resolverNombreOportunidad(seg.getOportunidadId());
        String fecha = seg.getFechaProgramada().format(FMT);
        String nombreCompleto = ejecutivo.getNombre() + " " + ejecutivo.getApellido();

        emailService.enviarAlertaVencido(
                ejecutivo.getEmail(), nombreCompleto,
                seg.getTipo(), nombreOp, fecha);

        String mensajeWa = String.format(
                "⚠️ *Seguimiento VENCIDO — CRM*\n\n%s, el siguiente seguimiento venció sin completarse:\n" +
                "• Oportunidad: %s\n• Tipo: %s\n• Fecha: %s\n\nIngresá al CRM para reprogramar o cerrarlo.",
                ejecutivo.getNombre(), nombreOp, seg.getTipo(), fecha);

        whatsAppService.enviar(ejecutivo.getTelefono(), mensajeWa);
    }

    private Usuario resolverEjecutivo(Long ejecutivoId) {
        return usuarioRepository.findById(ejecutivoId).orElseGet(() -> {
            log.warn("Ejecutivo {} no encontrado; se omite la notificación del seguimiento", ejecutivoId);
            return null;
        });
    }

    private String resolverNombreOportunidad(Long oportunidadId) {
        return oportunidadRepository.findById(oportunidadId)
                .map(op -> op.getNombre())
                .orElse("(oportunidad " + oportunidadId + ")");
    }
}
