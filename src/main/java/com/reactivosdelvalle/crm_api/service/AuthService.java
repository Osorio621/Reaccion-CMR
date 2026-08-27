package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.LoginRequest;
import com.reactivosdelvalle.crm_api.dto.request.RefreshTokenRequest;
import com.reactivosdelvalle.crm_api.dto.response.LoginResponse;
import com.reactivosdelvalle.crm_api.dto.response.UsuarioResponse;
import com.reactivosdelvalle.crm_api.entity.TokenReseteo;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.UsuarioMapper;
import com.reactivosdelvalle.crm_api.repository.TokenReseteoRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import com.reactivosdelvalle.crm_api.security.CustomUserDetailsService;
import com.reactivosdelvalle.crm_api.security.JwtUtils;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UsuarioRepository usuarioRepository;
    private final CustomUserDetailsService userDetailsService;
    private final UsuarioMapper usuarioMapper;
    private final TokenReseteoRepository tokenReseteoRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public AuthService(
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            UsuarioRepository usuarioRepository,
            CustomUserDetailsService userDetailsService,
            UsuarioMapper usuarioMapper,
            TokenReseteoRepository tokenReseteoRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.usuarioRepository = usuarioRepository;
        this.userDetailsService = userDetailsService;
        this.usuarioMapper = usuarioMapper;
        this.tokenReseteoRepository = tokenReseteoRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Authenticate credentials
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException ex) {
            log.warn("LOGIN RECHAZADO: usuario desactivado - {}", request.getEmail());
            throw ex;
        } catch (BadCredentialsException ex) {
            log.warn("LOGIN FALLIDO: credenciales incorrectas - {}", request.getEmail());
            throw ex;
        }

        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        log.info("LOGIN EXITOSO: {} ({} - rol {})", principal.getUsername(),
                principal.getId(), principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Update last activity timestamp
        Usuario usuario = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new AppException("Usuario no encontrado", HttpStatus.NOT_FOUND));
        
        usuario.setUltimaActividad(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(principal);
        String refreshToken = jwtUtils.generateRefreshToken(principal);

        UsuarioResponse usuarioResponse = usuarioMapper.toResponse(usuario);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .usuario(usuarioResponse)
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        
        if (!jwtUtils.validateToken(token)) {
            throw new AppException("Refresh token inválido o expirado", HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO");
        }

        String userIdStr = jwtUtils.getUserIdFromToken(token);
        Long userId = Long.parseLong(userIdStr);

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new AppException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        if (!usuario.getActivo()) {
            log.warn("REFRESH RECHAZADO: usuario desactivado - id {}", userId);
            throw new AppException("Usuario inactivo", HttpStatus.FORBIDDEN, "USUARIO_INACTIVO");
        }
        usuario.setUltimaActividad(LocalDateTime.now());
        usuarioRepository.save(usuario);

        UsuarioPrincipal principal = UsuarioPrincipal.create(usuario);
        
        String newAccessToken = jwtUtils.generateAccessToken(principal);
        String newRefreshToken = jwtUtils.generateRefreshToken(principal); // Rotated refresh token

        UsuarioResponse usuarioResponse = usuarioMapper.toResponse(usuario);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken) // Rotated refresh token
                .tokenType("Bearer")
                .usuario(usuarioResponse)
                .build();
    }

    /**
     * Genera un token de un solo uso (30 min) para restablecer la contraseña y
     * lo envía por correo. La respuesta al cliente siempre es genérica: no se
     * revela si el correo existe o no en el sistema.
     */
    @Transactional
    public void solicitarReseteoPassword(String email) {
        String emailNormalizado = email.trim().toLowerCase();

        // Higiene: eliminar tokens vencidos de solicitudes anteriores
        tokenReseteoRepository.deleteByExpiraEnBefore(LocalDateTime.now());

        Optional<Usuario> encontrado = usuarioRepository.findByEmail(emailNormalizado);
        if (encontrado.isEmpty() || !Boolean.TRUE.equals(encontrado.get().getActivo())) {
            log.info("SOLICITUD RESET: sin cuenta activa para {} (respuesta generica)", emailNormalizado);
            return;
        }

        Usuario usuario = encontrado.get();
        tokenReseteoRepository.deleteByUsuarioId(usuario.getId()); // un solo token vigente

        String token = generarToken();
        TokenReseteo registro = TokenReseteo.builder()
                .usuarioId(usuario.getId())
                .tokenHash(hashSha256(token))
                .expiraEn(LocalDateTime.now().plusMinutes(30))
                .build();
        tokenReseteoRepository.save(registro);

        log.info("SOLICITUD RESET: token generado para usuario id {}", usuario.getId());
        emailService.enviarCorreoReseteo(usuario.getEmail(), usuario.getNombre(), token);
    }

    /**
     * Valida el token, actualiza la contraseña y marca el token como usado.
     */
    @Transactional
    public void restablecerPassword(String token, String nuevaPassword) {
        String hash = hashSha256(token);

        TokenReseteo registro = tokenReseteoRepository.findByTokenHashAndUsadoFalse(hash)
                .orElseThrow(() -> new AppException(
                        "El enlace de restablecimiento no es válido o ya fue utilizado",
                        HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO"));

        if (registro.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new AppException(
                    "El enlace de restablecimiento expiró. Solicita uno nuevo.",
                    HttpStatus.BAD_REQUEST, "TOKEN_EXPIRADO");
        }

        Usuario usuario = usuarioRepository.findById(registro.getUsuarioId())
                .orElseThrow(() -> new AppException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setUltimaActividad(LocalDateTime.now());
        usuarioRepository.save(usuario);

        registro.setUsado(true);
        tokenReseteoRepository.save(registro);

        log.info("PASSWORD RESTABLECIDO: usuario id {} ({})", usuario.getId(), usuario.getEmail());
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashSha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
