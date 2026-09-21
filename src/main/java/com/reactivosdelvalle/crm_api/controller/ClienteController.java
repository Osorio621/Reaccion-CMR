package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.ClienteRequest;
import com.reactivosdelvalle.crm_api.dto.request.ContactoRequest;
import com.reactivosdelvalle.crm_api.dto.response.ClienteResponse;
import com.reactivosdelvalle.crm_api.dto.response.ContactoResponse;
import com.reactivosdelvalle.crm_api.dto.response.SyncTicketResponse;
import com.reactivosdelvalle.crm_api.service.ClienteService;
import com.reactivosdelvalle.crm_api.service.TicketsIntegrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final TicketsIntegrationService ticketsIntegrationService;

    @Autowired
    public ClienteController(ClienteService clienteService, TicketsIntegrationService ticketsIntegrationService) {
        this.clienteService = clienteService;
        this.ticketsIntegrationService = ticketsIntegrationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClienteResponse>> findAll() {
        return ResponseEntity.ok(clienteService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClienteResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.findById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClienteResponse> create(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse response = clienteService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClienteResponse> update(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        clienteService.delete(id);
        return ResponseEntity.ok(Map.of("mensaje", "Cliente eliminado correctamente"));
    }

    @PatchMapping("/{id}/reasignar")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<ClienteResponse> reasignar(@PathVariable Long id, @RequestParam Long nuevoEjecutivoId) {
        return ResponseEntity.ok(clienteService.reasignar(id, nuevoEjecutivoId));
    }

    /** Boton "Enviar cliente a tickets": envia (o reenvia) el cliente al sistema Next.js. */
    @PostMapping("/{id}/enviar-tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SyncTicketResponse> enviarTickets(@PathVariable Long id) {
        return ResponseEntity.ok(ticketsIntegrationService.enviarCliente(id));
    }

    /** Estado de sincronizacion con tickets (para badge "Enviado"/"Error" en el front). */
    @GetMapping("/{id}/tickets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SyncTicketResponse> estadoTickets(@PathVariable Long id) {
        return ResponseEntity.ok(ticketsIntegrationService.consultarEstado(id));
    }

    @GetMapping("/{id}/contactos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ContactoResponse>> findContactos(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.findContactos(id));
    }

    @PostMapping("/{id}/contactos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContactoResponse> createContacto(@PathVariable Long id, @Valid @RequestBody ContactoRequest request) {
        ContactoResponse response = clienteService.createContacto(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/contactos/{contactoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContactoResponse> updateContacto(
            @PathVariable Long id,
            @PathVariable Long contactoId,
            @Valid @RequestBody ContactoRequest request) {
        return ResponseEntity.ok(clienteService.updateContacto(id, contactoId, request));
    }

    @DeleteMapping("/{id}/contactos/{contactoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> deleteContacto(@PathVariable Long id, @PathVariable Long contactoId) {
        clienteService.deleteContacto(id, contactoId);
        return ResponseEntity.ok(Map.of("mensaje", "Contacto eliminado correctamente"));
    }
}