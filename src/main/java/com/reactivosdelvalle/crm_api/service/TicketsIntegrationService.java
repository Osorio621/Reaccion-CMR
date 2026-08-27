package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.TicketClientePayload;
import com.reactivosdelvalle.crm_api.dto.response.SyncTicketResponse;
import com.reactivosdelvalle.crm_api.entity.Cliente;
import com.reactivosdelvalle.crm_api.entity.SyncTicket;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.repository.ClienteRepository;
import com.reactivosdelvalle.crm_api.repository.SyncTicketRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Integracion CRM -&gt; Sistema de Tickets (Next.js).
 *
 * El envio se dispara manualmente con el boton "Enviar cliente a tickets"
 * (POST /api/clientes/{id}/enviar-tickets). Si el otro sistema no responde,
 * el registro queda en ERROR/PENDIENTE y un scheduler reintenta hasta
 * max-reintentos veces sin bloquear ni perder datos del CRM.
 */
@Service
public class TicketsIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(TicketsIntegrationService.class);
    private static final String ORIGEN = "CRM_REACTIVOS_DEL_VALLE";

    private final SyncTicketRepository syncTicketRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.tickets.enabled:false}")
    private boolean enabled;

    @Value("${app.tickets.url:http://localhost:3000/api/clientes}")
    private String url;

    @Value("${app.tickets.api-key:cambiar-en-produccion}")
    private String apiKey;

    @Value("${app.tickets.max-reintentos:5}")
    private int maxReintentos;

    @Value("${app.tickets.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${app.tickets.read-timeout-ms:10000}")
    private int readTimeoutMs;

    private RestClient restClient;

    public TicketsIntegrationService(SyncTicketRepository syncTicketRepository,
                                     ClienteRepository clienteRepository,
                                     UsuarioRepository usuarioRepository) {
        this.syncTicketRepository = syncTicketRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Envia (o reenvia) un cliente al sistema de tickets. Upsert por crmClienteId. */
    public SyncTicketResponse enviarCliente(Long clienteId) {
        if (!enabled) {
            throw new AppException("La integración con el sistema de tickets no está habilitada",
                    HttpStatus.BAD_REQUEST, "TICKETS_DESHABILITADO");
        }

        Cliente cliente = clienteRepository.findByIdAndActivoTrue(clienteId)
                .orElseThrow(() -> new AppException("Cliente no encontrado",
                        HttpStatus.NOT_FOUND, "CLIENTE_NO_ENCONTRADO"));

        SyncTicket sync = syncTicketRepository.findByClienteId(clienteId)
                .orElseGet(() -> SyncTicket.builder()
                        .clienteId(clienteId)
                        .estado(SyncTicket.PENDIENTE)
                        .intentos(0)
                        .build());

        LocalDateTime ahora = LocalDateTime.now();
        sync.setFechaUltimoIntento(ahora);
        sync.setIntentos(sync.getIntentos() + 1);

        try {
            Long ticketClienteId = postCliente(cliente);
            sync.setEstado(SyncTicket.ENVIADO);
            sync.setTicketClienteId(ticketClienteId);
            sync.setUltimoError(null);
            sync.setEnviadoEn(ahora);
            log.info("TICKETS SYNC OK: cliente {} sincronizado como ticket_cliente_id {} (intento {})",
                    clienteId, ticketClienteId, sync.getIntentos());
        } catch (Exception e) {
            sync.setEstado(SyncTicket.ERROR);
            String mensaje = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            // Los mensajes de error pueden traer la URL con api-key: no los guardamos completos
            sync.setUltimoError(abreviar(mensaje));
            log.warn("TICKETS SYNC FALLO: cliente {} intento {} de {} - {}",
                    clienteId, sync.getIntentos(), maxReintentos, abreviar(mensaje));
        }

        syncTicketRepository.save(sync);
        return aRespuesta(sync);
    }

    /** Estado actual de sincronizacion (para pintar el boton/badge en el front). */
    public SyncTicketResponse consultarEstado(Long clienteId) {
        return syncTicketRepository.findByClienteId(clienteId)
                .map(this::aRespuesta)
                .orElse(new SyncTicketResponse(clienteId, "NO_ENVIADO", null, 0, null, null, null));
    }

    /** Reintenta clientes que quedaron pendientes o con error, hasta max-reintentos. */
    public void reintentarPendientes() {
        if (!enabled) {
            return;
        }
        List<SyncTicket> pendientes = syncTicketRepository
                .findByEstadoIn(List.of(SyncTicket.PENDIENTE, SyncTicket.ERROR));

        int procesados = 0;
        for (SyncTicket sync : pendientes) {
            if (sync.getIntentos() >= maxReintentos) {
                continue;
            }
            try {
                enviarCliente(sync.getClienteId());
                procesados++;
            } catch (Exception e) {
                log.warn("TICKETS RETRY: cliente {} sigue sin sincronizar - {}",
                        sync.getClienteId(), abreviar(e.getMessage()));
            }
        }

        if (!pendientes.isEmpty()) {
            log.info("=== TICKETS RETRY: {} pendiente(s) revisado(s), {} reintento(s) exitoso(s) ===",
                    pendientes.size(), procesados);
        }
    }

    private Long postCliente(Cliente cliente) {
        TicketClientePayload payload = construirPayload(cliente);

        Map<?, ?> respuesta = obtenerRestClient()
                .post()
                .uri(url)
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (respuesta == null) {
            throw new IllegalStateException("El sistema de tickets no devolvió cuerpo de respuesta");
        }
        Object id = respuesta.get("ticketClienteId") != null
                ? respuesta.get("ticketClienteId")
                : respuesta.get("id");
        return id instanceof Number n ? n.longValue() : null;
    }

    private TicketClientePayload construirPayload(Cliente cliente) {
        String emailEjecutivo = usuarioRepository.findById(cliente.getEjecutivoId())
                .map(Usuario::getEmail)
                .orElse(null);

        return new TicketClientePayload(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getRazonSocial(),
                cliente.getRfc(),
                cliente.getEmailPrincipal(),
                cliente.getTelefonoPrincipal(),
                cliente.getDireccion(),
                cliente.getCiudad(),
                cliente.getEstadoRegion(),
                cliente.getSitioWeb(),
                emailEjecutivo,
                ORIGEN,
                LocalDateTime.now());
    }

    private RestClient obtenerRestClient() {
        if (restClient == null) {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                    .build();
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
            restClient = RestClient.builder().requestFactory(factory).build();
        }
        return restClient;
    }

    private String abreviar(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        return mensaje.length() > 300 ? mensaje.substring(0, 300) + "..." : mensaje;
    }

    private SyncTicketResponse aRespuesta(SyncTicket sync) {
        return new SyncTicketResponse(
                sync.getClienteId(),
                sync.getEstado(),
                sync.getTicketClienteId(),
                sync.getIntentos(),
                sync.getUltimoError(),
                sync.getFechaUltimoIntento(),
                sync.getEnviadoEn());
    }
}
