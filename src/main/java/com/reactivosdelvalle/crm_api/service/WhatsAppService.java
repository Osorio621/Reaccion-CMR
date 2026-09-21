package com.reactivosdelvalle.crm_api.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Envío de mensajes WhatsApp vía Twilio.
 *
 * app.whatsapp.enabled=false → loguea el mensaje (modo dev/fallback)
 * app.whatsapp.enabled=true  → envía a través de la API de Twilio
 *
 * El fallo de envío nunca interrumpe el flujo de negocio.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    @Value("${app.whatsapp.enabled:false}")
    private boolean habilitado;

    @Value("${app.whatsapp.account-sid:changeme}")
    private String accountSid;

    @Value("${app.whatsapp.auth-token:changeme}")
    private String authToken;

    // Número Twilio emisor con prefijo whatsapp:
    // Sandbox: whatsapp:+14155238886 | Producción: whatsapp:+57XXXXXXXXXX
    @Value("${app.whatsapp.from:whatsapp:+14155238886}")
    private String from;

    @PostConstruct
    void init() {
        if (habilitado) {
            Twilio.init(accountSid, authToken);
            log.info("WhatsAppService inicializado con cuenta Twilio {}", accountSid);
        }
    }

    public void enviar(String telefono, String mensaje) {
        String destino = normalizarTelefono(telefono);
        if (destino == null) {
            log.warn("WhatsApp omitido — número inválido o vacío: '{}'", telefono);
            return;
        }

        if (!habilitado) {
            log.info("WHATSAPP (simulado) → {} : {}", destino, mensaje);
            return;
        }

        try {
            Message.creator(
                    new PhoneNumber("whatsapp:" + destino),
                    new PhoneNumber(from),
                    mensaje
            ).create();
            log.info("WhatsApp enviado a {}", destino);
        } catch (Exception ex) {
            log.error("No se pudo enviar WhatsApp a {}: {}", destino, ex.getMessage());
        }
    }

    /**
     * Normaliza teléfonos colombianos a formato E.164 (+57XXXXXXXXXX).
     * Retorna null si no puede determinar el formato.
     *
     * Entradas aceptadas:
     *  - 3XX XXXXXXX  (10 dígitos, móvil colombiano)
     *  - 573XXXXXXXXX (12 dígitos con prefijo país, sin +)
     *  - +573XXXXXXXX (ya en E.164)
     *  - 00573XXXXXXXX (con 00 en vez de +)
     */
    String normalizarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) return null;

        String limpio = telefono.replaceAll("[\\s\\-().]", "");

        if (limpio.startsWith("+")) {
            return limpio.length() >= 10 ? limpio : null;
        }
        if (limpio.startsWith("0057") && limpio.length() == 14) {
            return "+" + limpio.substring(2);
        }
        if (limpio.startsWith("57") && limpio.length() == 12) {
            return "+" + limpio;
        }
        if (limpio.startsWith("3") && limpio.length() == 10) {
            return "+57" + limpio;
        }
        return null;
    }
}
