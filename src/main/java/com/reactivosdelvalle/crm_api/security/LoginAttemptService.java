package com.reactivosdelvalle.crm_api.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rastreo de intentos de login fallidos en memoria.
 * Bloquea la cuenta durante BLOQUEO_MINUTOS después de MAX_INTENTOS consecutivos.
 * El bloqueo se limpia automáticamente al expirar o al primer login exitoso.
 */
@Component
public class LoginAttemptService {

    static final int MAX_INTENTOS    = 5;
    static final int BLOQUEO_MINUTOS = 15;

    private record Info(int intentos, Instant bloqueadoHasta) {}

    private final ConcurrentHashMap<String, Info> mapa = new ConcurrentHashMap<>();

    public void registrarFallo(String email) {
        mapa.merge(email.toLowerCase(), new Info(1, null), (prev, nuevo) -> {
            int count = prev.intentos() + 1;
            Instant hasta = prev.bloqueadoHasta() != null
                    ? prev.bloqueadoHasta()
                    : (count >= MAX_INTENTOS
                            ? Instant.now().plus(BLOQUEO_MINUTOS, ChronoUnit.MINUTES)
                            : null);
            return new Info(count, hasta);
        });
    }

    public void registrarExito(String email) {
        mapa.remove(email.toLowerCase());
    }

    public boolean estaBloqueado(String email) {
        Info info = mapa.get(email.toLowerCase());
        if (info == null || info.bloqueadoHasta() == null) return false;
        if (Instant.now().isAfter(info.bloqueadoHasta())) {
            mapa.remove(email.toLowerCase());
            return false;
        }
        return true;
    }

    public long minutosRestantes(String email) {
        Info info = mapa.get(email.toLowerCase());
        if (info == null || info.bloqueadoHasta() == null) return 0;
        return Math.max(1, ChronoUnit.MINUTES.between(Instant.now(), info.bloqueadoHasta()) + 1);
    }
}
