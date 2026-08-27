package com.reactivosdelvalle.crm_api.controller;

import com.reactivosdelvalle.crm_api.dto.request.LoginRequest;
import com.reactivosdelvalle.crm_api.dto.request.OlvidePasswordRequest;
import com.reactivosdelvalle.crm_api.dto.request.RefreshTokenRequest;
import com.reactivosdelvalle.crm_api.dto.request.RestablecerPasswordRequest;
import com.reactivosdelvalle.crm_api.dto.response.LoginResponse;
import com.reactivosdelvalle.crm_api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        LoginResponse response = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        SecurityContextHolder.clearContext();
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Sesión cerrada correctamente");
        return ResponseEntity.ok(response);
    }

    /**
     * Paso 1 del flujo "¿Olvidaste tu contraseña?". La respuesta es genérica
     * a propósito: no se revela si el correo está o no registrado.
     */
    @PostMapping("/olvide-password")
    public ResponseEntity<Map<String, String>> olvidePassword(
            @Valid @RequestBody OlvidePasswordRequest request) {
        authService.solicitarReseteoPassword(request.getEmail());
        Map<String, String> response = new HashMap<>();
        response.put("mensaje",
                "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.");
        return ResponseEntity.ok(response);
    }

    /**
     * Paso 2: restablecer la contraseña con el token recibido por correo.
     */
    @PostMapping("/restablecer-password")
    public ResponseEntity<Map<String, String>> restablecerPassword(
            @Valid @RequestBody RestablecerPasswordRequest request) {
        authService.restablecerPassword(request.getToken(), request.getNuevaPassword());
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Contraseña actualizada correctamente. Ya puedes iniciar sesión.");
        return ResponseEntity.ok(response);
    }
}
