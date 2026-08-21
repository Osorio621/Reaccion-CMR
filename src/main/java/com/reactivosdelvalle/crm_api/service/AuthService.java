package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.LoginRequest;
import com.reactivosdelvalle.crm_api.dto.request.RefreshTokenRequest;
import com.reactivosdelvalle.crm_api.dto.response.LoginResponse;
import com.reactivosdelvalle.crm_api.dto.response.UsuarioResponse;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.UsuarioMapper;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import com.reactivosdelvalle.crm_api.security.CustomUserDetailsService;
import com.reactivosdelvalle.crm_api.security.JwtUtils;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UsuarioRepository usuarioRepository;
    private final CustomUserDetailsService userDetailsService;
    private final UsuarioMapper usuarioMapper;

    @Autowired
    public AuthService(
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            UsuarioRepository usuarioRepository,
            CustomUserDetailsService userDetailsService,
            UsuarioMapper usuarioMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.usuarioRepository = usuarioRepository;
        this.userDetailsService = userDetailsService;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Authenticate credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();

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
            throw new AppException("Usuario inactivo", HttpStatus.FORBIDDEN, "USUARIO_INACTIVO");
        }

        // Update last activity on refresh token too
        usuario.setUltimaActividad(LocalDateTime.now());
        usuarioRepository.save(usuario);

        UsuarioPrincipal principal = UsuarioPrincipal.create(usuario);
        
        String newAccessToken = jwtUtils.generateAccessToken(principal);
        String newRefreshToken = jwtUtils.generateRefreshToken(principal); // Rotated refresh token

        UsuarioResponse usuarioResponse = usuarioMapper.toResponse(usuario);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .usuario(usuarioResponse)
                .build();
    }
}
