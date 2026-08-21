package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.response.AuditoriaResponse;
import com.reactivosdelvalle.crm_api.entity.Auditoria;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.mapper.AuditoriaMapper;
import com.reactivosdelvalle.crm_api.repository.AuditoriaRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditoriaService {

    private static final int LIMITE_DEFECTO = 100;
    private static final int LIMITE_MAXIMO = 500;

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaMapper auditoriaMapper;

    @Autowired
    public AuditoriaService(
            AuditoriaRepository auditoriaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaMapper auditoriaMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaMapper = auditoriaMapper;
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listarUltimos(Integer limite) {
        Pageable pageable = PageRequest.of(0, normalizarLimite(limite));
        return mapear(auditoriaRepository.findAllByOrderByCreatedAtDescIdDesc(pageable));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> historialRegistro(String tabla, Long registroId) {
        return mapear(auditoriaRepository.findByTablaNombreAndRegistroIdOrderByCreatedAtDescIdDesc(tabla, registroId));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> porUsuario(Long usuarioId, Integer limite) {
        Pageable pageable = PageRequest.of(0, normalizarLimite(limite));
        return mapear(auditoriaRepository.findByUsuarioIdOrderByCreatedAtDescIdDesc(usuarioId, pageable));
    }

    private List<AuditoriaResponse> mapear(List<Auditoria> registros) {
        List<Long> usuarioIds = registros.stream()
                .map(Auditoria::getUsuarioId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, String> usuariosNombres = usuarioIds.isEmpty()
                ? Map.of()
                : usuarioRepository.findAllById(usuarioIds).stream()
                        .collect(Collectors.toMap(
                                Usuario::getId,
                                u -> u.getNombre() + " " + u.getApellido()));

        return registros.stream()
                .map(r -> auditoriaMapper.toResponse(r, usuariosNombres))
                .toList();
    }

    private int normalizarLimite(Integer limite) {
        if (limite == null || limite <= 0) {
            return LIMITE_DEFECTO;
        }
        return Math.min(limite, LIMITE_MAXIMO);
    }
}
