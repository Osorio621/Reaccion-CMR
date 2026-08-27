package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.ActualizarPerfilRequest;
import com.reactivosdelvalle.crm_api.dto.request.CambiarPasswordRequest;
import com.reactivosdelvalle.crm_api.dto.request.UsuarioRequest;
import com.reactivosdelvalle.crm_api.dto.response.UsuarioResponse;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.UsuarioMapper;
import com.reactivosdelvalle.crm_api.repository.TokenReseteoRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final TokenReseteoRepository tokenReseteoRepository;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          UsuarioMapper usuarioMapper, TokenReseteoRepository tokenReseteoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
        this.tokenReseteoRepository = tokenReseteoRepository;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll().stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse findById(Long id) {
        return usuarioMapper.toResponse(getUsuario(id));
    }

    @Transactional
    public UsuarioResponse create(UsuarioRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException("Ya existe un usuario con el correo: " + request.getEmail(), HttpStatus.CONFLICT, "EMAIL_DUPLICADO");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AppException("La contraseña es obligatoria", HttpStatus.BAD_REQUEST, "PASSWORD_OBLIGATORIA");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol())
                .telefono(request.getTelefono())
                .fotoUrl(request.getFotoUrl())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse update(Long id, UsuarioRequest request) {
        Usuario usuario = getUsuario(id);

        usuarioRepository.findByEmail(request.getEmail())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new AppException("Ya existe un usuario con el correo: " + request.getEmail(), HttpStatus.CONFLICT, "EMAIL_DUPLICADO");
                });

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setEmail(request.getEmail());
        usuario.setRol(request.getRol());
        usuario.setTelefono(request.getTelefono());
        usuario.setFotoUrl(request.getFotoUrl());
        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void delete(Long id) {
        Usuario usuario = getUsuario(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    // ===== Perfil propio (cualquier usuario autenticado) =====

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfil(Long usuarioId) {
        return usuarioMapper.toResponse(getUsuario(usuarioId));
    }

    /**
     * El usuario edita SOLO sus datos personales: nunca rol, email ni activo.
     */
    @Transactional
    public UsuarioResponse actualizarPerfil(Long usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = getUsuario(usuarioId);
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setTelefono(request.getTelefono());
        usuario.setFotoUrl(request.getFotoUrl());
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    /**
     * Cambio de contraseña con verificación de la actual. Invalida además
     * cualquier token de restablecimiento pendiente.
     */
    @Transactional
    public void cambiarPassword(Long usuarioId, CambiarPasswordRequest request) {
        Usuario usuario = getUsuario(usuarioId);

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new AppException("La contraseña actual es incorrecta",
                    HttpStatus.BAD_REQUEST, "PASSWORD_ACTUAL_INCORRECTA");
        }
        if (passwordEncoder.matches(request.getNuevaPassword(), usuario.getPasswordHash())) {
            throw new AppException("La nueva contraseña debe ser diferente a la actual",
                    HttpStatus.BAD_REQUEST, "PASSWORD_IGUAL");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);
        tokenReseteoRepository.deleteByUsuarioId(usuarioId);
    }

    private Usuario getUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new AppException("Usuario no encontrado con id: " + id, HttpStatus.NOT_FOUND));
    }
}