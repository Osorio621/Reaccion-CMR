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
