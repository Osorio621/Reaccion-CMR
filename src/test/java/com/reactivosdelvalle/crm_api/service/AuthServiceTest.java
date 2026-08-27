package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.LoginRequest;
import com.reactivosdelvalle.crm_api.dto.response.UsuarioResponse;
import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.TokenReseteo;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.UsuarioMapper;
import com.reactivosdelvalle.crm_api.repository.TokenReseteoRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import com.reactivosdelvalle.crm_api.security.CustomUserDetailsService;
import com.reactivosdelvalle.crm_api.security.JwtUtils;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de autenticación: login y flujo de
 * restablecimiento de contraseña.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private TokenReseteoRepository tokenReseteoRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    private Usuario usuarioAdmin;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtUtils, usuarioRepository,
                userDetailsService, usuarioMapper, tokenReseteoRepository,
                emailService, passwordEncoder);

        usuarioAdmin = Usuario.builder()
                .id(1L)
                .nombre("Administrador")
                .apellido("Sistema")
                .email("admin@reactivosdelvalle.com")
                .passwordHash("$2a$hash")
                .rol(RolUsuario.ADMIN)
                .activo(true)
                .build();
    }

    // ===== LOGIN =====

    @Test
    @DisplayName("Login exitoso devuelve tokens y actualiza última actividad")
    void loginExitoso() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@reactivosdelvalle.com");
        request.setPassword("Admin123!");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(UsuarioPrincipal.create(usuarioAdmin));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAdmin));
        when(jwtUtils.generateAccessToken(any())).thenReturn("token-acceso");
        when(jwtUtils.generateRefreshToken(any())).thenReturn("token-refresco");

        var response = authService.login(request);

        assertAll(
                () -> assertEquals("token-acceso", response.getAccessToken()),
                () -> assertEquals("token-refresco", response.getRefreshToken()),
                () -> assertEquals("Bearer", response.getTokenType())
        );

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertNotNull(captor.getValue().getUltimaActividad(), "El login debe registrar la última actividad");
    }

    @Test
    @DisplayName("Login con credenciales incorrectas lanza BadCredentialsException")
    void loginCredencialesIncorrectas() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@reactivosdelvalle.com");
        request.setPassword("incorrecta");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login de usuario desactivado lanza DisabledException")
    void loginUsuarioDesactivado() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inactivo@reactivosdelvalle.com");
        request.setPassword("loquesea");

        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThrows(DisabledException.class, () -> authService.login(request));
    }

    // ===== RECUPERACIÓN DE CONTRASEÑA =====

    @Test
    @DisplayName("Solicitud de reseteo con email inexistente no guarda nada ni falla")
    void solicitarReseteoEmailInexistente() {
        when(usuarioRepository.findByEmail("fantasma@x.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.solicitarReseteoPassword("fantasma@x.com"));
        verify(tokenReseteoRepository, never()).save(any());
        verify(emailService, never()).enviarCorreoReseteo(any(), any(), any());
    }

    @Test
    @DisplayName("Solicitud de reseteo genera token hasheado con 30 min de validez")
    void solicitarReseteoGeneraTokenValido() {
        when(usuarioRepository.findByEmail("admin@reactivosdelvalle.com"))
                .thenReturn(Optional.of(usuarioAdmin));

        LocalDateTime antes = LocalDateTime.now();
        authService.solicitarReseteoPassword("admin@reactivosdelvalle.com");
        LocalDateTime despues = LocalDateTime.now();

        ArgumentCaptor<TokenReseteo> captorToken = ArgumentCaptor.forClass(TokenReseteo.class);
        verify(tokenReseteoRepository).save(captorToken.capture());

        ArgumentCaptor<String> captorClaro = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCorreoReseteo(eq("admin@reactivosdelvalle.com"),
                eq("Administrador"), captorClaro.capture());

        TokenReseteo guardado = captorToken.getValue();
        String tokenClaro = captorClaro.getValue();

        assertAll(
                // Nunca se guarda el token en claro
                () -> assertNotEquals(tokenClaro, guardado.getTokenHash()),
                () -> assertEquals(64, guardado.getTokenHash().length(), "SHA-256 en hex son 64 caracteres"),
                // Validez de 30 minutos (con margen por la ejecución)
                () -> assertTrue(guardado.getExpiraEn().isAfter(antes.plusMinutes(29))),
                () -> assertTrue(guardado.getExpiraEn().isBefore(despues.plusMinutes(31))),
                () -> assertEquals(1L, guardado.getUsuarioId()),
                // Se limpian tokens previos del mismo usuario
                () -> verify(tokenReseteoRepository).deleteByUsuarioId(1L)
        );
    }

    @Test
    @DisplayName("Restablecimiento con token inválido es rechazado")
    void restablecerTokenInvalido() {
        when(tokenReseteoRepository.findByTokenHashAndUsadoFalse(any()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> authService.restablecerPassword("token-falso", "NuevaPass123"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Restablecimiento con token expirado es rechazado")
    void restablecerTokenExpirado() {
        TokenReseteo vencido = TokenReseteo.builder()
                .usuarioId(1L)
                .tokenHash("hash")
                .expiraEn(LocalDateTime.now().minusMinutes(5))
                .usado(false)
                .build();
        when(tokenReseteoRepository.findByTokenHashAndUsadoFalse(any()))
                .thenReturn(Optional.of(vencido));

        AppException ex = assertThrows(AppException.class,
                () -> authService.restablecerPassword("cualquiera", "NuevaPass123"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Restablecimiento válido actualiza la contraseña y marca el token como usado")
    void restablecerOk() {
        TokenReseteo registro = TokenReseteo.builder()
                .usuarioId(1L)
                .tokenHash("hash")
                .expiraEn(LocalDateTime.now().plusMinutes(20))
                .usado(false)
                .build();
        when(tokenReseteoRepository.findByTokenHashAndUsadoFalse(any()))
                .thenReturn(Optional.of(registro));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAdmin));
        when(passwordEncoder.encode("NuevaPass123")).thenReturn("$2a$nuevo-hash");

        authService.restablecerPassword("token-claro", "NuevaPass123");

        ArgumentCaptor<Usuario> captorUsuario = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<TokenReseteo> captorRegistro = ArgumentCaptor.forClass(TokenReseteo.class);

        assertAll(
                () -> verify(usuarioRepository).save(captorUsuario.capture()),
                () -> assertEquals("$2a$nuevo-hash", captorUsuario.getValue().getPasswordHash()),
                () -> verify(tokenReseteoRepository).save(captorRegistro.capture()),
                () -> assertTrue(captorRegistro.getValue().getUsado(), "El token debe quedar marcado como usado")
        );
    }
}
