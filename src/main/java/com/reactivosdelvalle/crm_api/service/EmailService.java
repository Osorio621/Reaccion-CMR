package com.reactivosdelvalle.crm_api.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envío de correos transaccionales.
 * - app.mail.enabled=true  -> envía por SMTP (requiere spring.mail.host configurado)
 * - app.mail.enabled=false -> modo desarrollo: registra el enlace en el log
 * El fallo del envío nunca interrumpe el flujo de negocio.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean correoHabilitado;

    @Value("${app.mail.from:no-reply@reactivosdelvalle.com}")
    private String remitente;

    @Value("${app.mail.reset-url-base:http://localhost:5173/restablecer-password}")
    private String urlBaseReseteo;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void enviarCorreoReseteo(String destinatario, String nombre, String token) {
        String enlace = urlBaseReseteo + "?token=" + token;

        if (!correoHabilitado) {
            // Modo desarrollo: sin SMTP configurado, el enlace queda visible en el log
            log.info("CORREO (simulado) para {} - enlace de restablecimiento: {}", destinatario, enlace);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("app.mail.enabled=true pero spring.mail.host no está configurado; no se puede enviar a {}", destinatario);
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Recupera tu contraseña - CRM Reactivos del Valle");
            helper.setText(plantillaReseteo(nombre, enlace), true);
            mailSender.send(mensaje);
            log.info("Correo de restablecimiento enviado a {}", destinatario);
        } catch (Exception ex) {
            log.error("No se pudo enviar el correo de restablecimiento a {}: {}", destinatario, ex.getMessage());
        }
    }

    public void enviarRecordatorio(String destinatario, String nombre,
                                    String tipoSeguimiento, String nombreOportunidad,
                                    String fechaProgramada) {
        if (!correoHabilitado) {
            log.info("CORREO (simulado) recordatorio para {} — {} / {} el {}",
                    destinatario, tipoSeguimiento, nombreOportunidad, fechaProgramada);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("app.mail.enabled=true pero JavaMailSender no está disponible");
            return;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Recordatorio: seguimiento mañana — " + nombreOportunidad);
            helper.setText(plantillaRecordatorio(nombre, tipoSeguimiento, nombreOportunidad, fechaProgramada), true);
            mailSender.send(mensaje);
            log.info("Correo de recordatorio enviado a {}", destinatario);
        } catch (Exception ex) {
            log.error("No se pudo enviar recordatorio a {}: {}", destinatario, ex.getMessage());
        }
    }

    public void enviarAlertaVencido(String destinatario, String nombre,
                                     String tipoSeguimiento, String nombreOportunidad,
                                     String fechaProgramada) {
        if (!correoHabilitado) {
            log.info("CORREO (simulado) alerta vencido para {} — {} / {} vencido el {}",
                    destinatario, tipoSeguimiento, nombreOportunidad, fechaProgramada);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("app.mail.enabled=true pero JavaMailSender no está disponible");
            return;
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Seguimiento VENCIDO — " + nombreOportunidad);
            helper.setText(plantillaVencido(nombre, tipoSeguimiento, nombreOportunidad, fechaProgramada), true);
            mailSender.send(mensaje);
            log.info("Correo de vencimiento enviado a {}", destinatario);
        } catch (Exception ex) {
            log.error("No se pudo enviar alerta de vencimiento a {}: {}", destinatario, ex.getMessage());
        }
    }

    private String plantillaRecordatorio(String nombre, String tipo, String oportunidad, String fecha) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:24px;">
                  <h2 style="color:#0b5394;">CRM Reactivos del Valle</h2>
                  <p>Hola %s,</p>
                  <p>Te recordamos que mañana tenés un seguimiento pendiente:</p>
                  <table style="border-collapse:collapse;width:100%%;margin:16px 0;">
                    <tr><td style="padding:8px;border:1px solid #ddd;font-weight:bold;">Oportunidad</td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;font-weight:bold;">Tipo</td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;font-weight:bold;">Fecha</td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                  </table>
                  <p style="font-size:13px;color:#666;">Accedé al CRM para ver los detalles y registrar la gestión.</p>
                </div>
                """.formatted(nombre, oportunidad, tipo, fecha);
    }

    private String plantillaVencido(String nombre, String tipo, String oportunidad, String fecha) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:24px;">
                  <h2 style="color:#c0392b;">CRM Reactivos del Valle — Seguimiento Vencido</h2>
                  <p>Hola %s,</p>
                  <p>El siguiente seguimiento <strong>venció sin ser completado</strong>:</p>
                  <table style="border-collapse:collapse;width:100%%;margin:16px 0;">
                    <tr><td style="padding:8px;border:1px solid #ddd;font-weight:bold;">Oportunidad</td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;font-weight:bold;">Tipo</td>
                        <td style="padding:8px;border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px;border:1px solid #ddd;font-weight:bold;">Fecha programada</td>
                        <td style="padding:8px;border:1px solid #ddd;color:#c0392b;">%s</td></tr>
                  </table>
                  <p style="font-size:13px;color:#666;">Ingresá al CRM para reprogramar o cerrar el seguimiento.</p>
                </div>
                """.formatted(nombre, oportunidad, tipo, fecha);
    }

    private String plantillaReseteo(String nombre, String enlace) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:24px;">
                  <h2 style="color:#0b5394;">CRM Reactivos del Valle</h2>
                  <p>Hola %s,</p>
                  <p>Recibimos una solicitud para restablecer tu contraseña.
                     Haz clic en el siguiente botón (válido por 30 minutos):</p>
                  <p style="text-align:center;margin:28px 0;">
                    <a href="%s" style="background:#0b5394;color:#ffffff;padding:12px 28px;
                       text-decoration:none;border-radius:6px;font-weight:bold;">Restablecer contraseña</a>
                  </p>
                  <p style="font-size:13px;color:#666;">Si no solicitaste este cambio, ignora este mensaje
                     y tu contraseña seguirá siendo la misma.</p>
                </div>
                """.formatted(nombre, enlace);
    }
}
