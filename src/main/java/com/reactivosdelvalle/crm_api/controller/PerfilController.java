package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.ActualizarPerfilRequest;
import com.reactivosdelvalle.crm_api.dto.request.CambiarPasswordRequest;
import com.reactivosdelvalle.crm_api.dto.response.UsuarioResponse;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.service.UsuarioService;
import com.reactivosdelvalle.crm_api.storage.ArchivoStorage;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Perfil propio del usuario autenticado (cualquier rol).
 * Los datos administrativos (rol, email, activo) se gestionan en /api/admin/usuarios.
 */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;
    private final ArchivoStorage storage;

    @Autowired
    public PerfilController(UsuarioService usuarioService, ArchivoStorage storage) {
        this.usuarioService = usuarioService;
        this.storage = storage;
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

    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioResponse> subirFoto(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam("foto") MultipartFile foto) {
        return ResponseEntity.ok(usuarioService.subirFoto(principal.getId(), foto));
    }

    // Endpoint público — sin auth, para que <img src> funcione sin token
    @GetMapping("/foto/{usuarioId}")
    public ResponseEntity<Resource> servirFoto(@PathVariable Long usuarioId) {
        String ruta = usuarioService.obtenerFotoRuta(usuarioId);
        if (ruta == null) {
            return ResponseEntity.notFound().build();
        }
        Resource recurso;
        try {
            recurso = storage.cargar(ruta);
        } catch (IOException ex) {
            return ResponseEntity.notFound().build();
        }
        String mime = ruta.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .body(recurso);
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
