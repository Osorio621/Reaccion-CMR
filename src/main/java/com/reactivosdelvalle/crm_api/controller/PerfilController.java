package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.ActualizarPerfilRequest;
import com.reactivosdelvalle.crm_api.dto.request.CambiarPasswordRequest;
import com.reactivosdelvalle.crm_api.dto.response.UsuarioResponse;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Perfil propio del usuario autenticado (cualquier rol).
 * Los datos administrativos (rol, email, activo) se gestionan en /api/admin/usuarios.
 */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;

    @Autowired
    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<UsuarioResponse> verPerfil(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioService.obtenerPerfil(principal.getId()));
    }

    @PutMapping
    public ResponseEntity<UsuarioResponse> actualizarPerfil(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(principal.getId(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CambiarPasswordRequest request) {
        usuarioService.cambiarPassword(principal.getId(), request);
        return ResponseEntity.ok(Map.of("mensaje",
                "Contraseña actualizada correctamente"));
    }
}
