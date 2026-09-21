package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.ActualizarPerfilRequest;
import com.reactivosdelvalle.crm_api.dto.request.CambiarPasswordRequest;
import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.UsuarioMapper;
import com.reactivosdelvalle.crm_api.repository.TokenReseteoRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del perfil propio: edición de datos y cambio de contraseña.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private TokenReseteoRepository tokenReseteoRepository;

    private PasswordEncoder passwordEncoder; // real: valida la lógica de contraseñas de verdad

    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder,
                usuarioMapper, tokenReseteoRepository);

        usuario = Usuario.builder()
                .id(7L)
                .nombre("Ana")
                .apellido("Gómez")
                .email("ana@reactivosdelvalle.com")
                .passwordHash(passwordEncoder.encode("Vieja1234"))
                .rol(RolUsuario.EJECUTIVO)
                .activo(true)
                .build();
    }

    // ===== CAMBIO DE CONTRASEÑA =====

    @Test
    @DisplayName("Cambio de contraseña con actual incorrecta es rechazado")
    void cambiarPasswordActualIncorrecta() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        CambiarPasswordRequest request = new CambiarPasswordRequest("equivocada", "Nueva12345");

        AppException ex = assertThrows(AppException.class,
                () -> usuarioService.cambiarPassword(7L, request));

        assertEquals("PASSWORD_ACTUAL_INCORRECTA", ex.getErrorKey());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambio de contraseña nueva igual a la actual es rechazado")
    void cambiarPasswordIgualALaActual() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        CambiarPasswordRequest request = new CambiarPasswordRequest("Vieja1234", "Vieja1234");

        AppException ex = assertThrows(AppException.class,
                () -> usuarioService.cambiarPassword(7L, request));

        assertEquals("PASSWORD_IGUAL", ex.getErrorKey());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambio de contraseña válido actualiza el hash e invalida tokens de reseteo")
    void cambiarPasswordOk() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        CambiarPasswordRequest request = new CambiarPasswordRequest("Vieja1234", "Nueva12345");

        String hashAnterior = usuario.getPasswordHash();
        usuarioService.cambiarPassword(7L, request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        Usuario guardado = captor.getValue();
        assertAll(
                () -> assertNotEquals(hashAnterior, guardado.getPasswordHash()),
                () -> assertTrue(passwordEncoder.matches("Nueva12345", guardado.getPasswordHash()),
                        "El nuevo hash debe corresponder a la nueva contraseña"),
                () -> assertFalse(passwordEncoder.matches("Vieja1234", guardado.getPasswordHash()),
                        "La contraseña anterior ya no debe servir"),
                () -> verify(tokenReseteoRepository).deleteByUsuarioId(7L)
        );
    }

    // ===== PERFIL PROPIO =====

    @Test
    @DisplayName("Actualizar perfil modifica solo datos personales")
    void actualizarPerfil() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        ActualizarPerfilRequest request = new ActualizarPerfilRequest(
                "Ana María", "Gómez Ruiz", "3105550000", "https://foto.url/ana.png");

        usuarioService.actualizarPerfil(7L, request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();

        assertAll(
                () -> assertEquals("Ana María", guardado.getNombre()),
                () -> assertEquals("Gómez Ruiz", guardado.getApellido()),
                () -> assertEquals("3105550000", guardado.getTelefono()),
                () -> assertEquals("https://foto.url/ana.png", guardado.getFotoUrl()),
                // Datos sensibles intactos
                () -> assertEquals("ana@reactivosdelvalle.com", guardado.getEmail()),
                () -> assertEquals(RolUsuario.EJECUTIVO, guardado.getRol()),
                () -> assertTrue(guardado.getActivo())
        );
    }

    @Test
    @DisplayName("Perfil inexistente lanza 404")
    void perfilInexistente() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> usuarioService.obtenerPerfil(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
