package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.ComunicacionRequest;
import com.reactivosdelvalle.crm_api.dto.response.ComunicacionResponse;
import com.reactivosdelvalle.crm_api.entity.Comunicacion;
import com.reactivosdelvalle.crm_api.entity.TipoEntidadComunicacion;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.repository.*;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComunicacionService {

    private final ComunicacionRepository comunicacionRepository;
    private final ClienteRepository clienteRepository;
    private final ProspectoRepository prospectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;

    public ComunicacionService(ComunicacionRepository comunicacionRepository,
                               ClienteRepository clienteRepository,
                               ProspectoRepository prospectoRepository,
                               UsuarioRepository usuarioRepository,
                               SecurityUtils securityUtils) {
        this.comunicacionRepository = comunicacionRepository;
        this.clienteRepository = clienteRepository;
        this.prospectoRepository = prospectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public ComunicacionResponse registrar(TipoEntidadComunicacion tipo, Long entidadId,
                                          ComunicacionRequest req) {
        verificarAcceso(tipo, entidadId);

        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        Comunicacion comunicacion = Comunicacion.builder()
                .entidadTipo(tipo)
                .entidadId(entidadId)
                .tipo(req.tipo())
                .fecha(req.fecha())
                .descripcion(req.descripcion())
                .resultado(req.resultado())
                .ejecutivoId(usuario != null ? usuario.getId() : null)
                .build();

        return mapear(comunicacionRepository.save(comunicacion));
    }

    @Transactional(readOnly = true)
    public List<ComunicacionResponse> listar(TipoEntidadComunicacion tipo, Long entidadId) {
        verificarAcceso(tipo, entidadId);

        List<Comunicacion> lista = comunicacionRepository
                .findByEntidadTipoAndEntidadIdOrderByFechaDesc(tipo, entidadId);

        Set<Long> ejecutivoIds = lista.stream()
                .filter(c -> c.getEjecutivoId() != null)
                .map(Comunicacion::getEjecutivoId)
                .collect(Collectors.toSet());

        Map<Long, String> nombresPorId = usuarioRepository.findAllById(ejecutivoIds)
                .stream()
                .collect(Collectors.toMap(Usuario::getId,
                        u -> u.getNombre() + " " + u.getApellido()));

        return lista.stream()
                .map(c -> mapearConNombre(c, nombresPorId.get(c.getEjecutivoId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public ComunicacionResponse actualizar(Long id, ComunicacionRequest req) {
        Comunicacion comunicacion = buscarOFallar(id);
        verificarAcceso(comunicacion.getEntidadTipo(), comunicacion.getEntidadId());

        comunicacion.setTipo(req.tipo());
        comunicacion.setFecha(req.fecha());
        comunicacion.setDescripcion(req.descripcion());
        comunicacion.setResultado(req.resultado());

        return mapear(comunicacionRepository.save(comunicacion));
    }

    @Transactional
    public void eliminar(Long id) {
        Comunicacion comunicacion = buscarOFallar(id);
        verificarAcceso(comunicacion.getEntidadTipo(), comunicacion.getEntidadId());
        comunicacionRepository.delete(comunicacion);
    }

    // --- Helpers ---

    private void verificarAcceso(TipoEntidadComunicacion tipo, Long entidadId) {
        if (securityUtils.esGerenteOAdmin()) return;

        Long propietarioId = obtenerPropietarioId(tipo, entidadId);
        if (!securityUtils.esPropietario(propietarioId)) {
            throw new AppException("Sin permiso para acceder a esta entidad", HttpStatus.FORBIDDEN);
        }
    }

    private Long obtenerPropietarioId(TipoEntidadComunicacion tipo, Long entidadId) {
        return switch (tipo) {
            case CLIENTE -> clienteRepository.findById(entidadId)
                    .orElseThrow(() -> new AppException("Cliente no encontrado", HttpStatus.NOT_FOUND))
                    .getEjecutivoId();
            case PROSPECTO -> prospectoRepository.findById(entidadId)
                    .orElseThrow(() -> new AppException("Prospecto no encontrado", HttpStatus.NOT_FOUND))
                    .getResponsableId();
        };
    }

    private Comunicacion buscarOFallar(Long id) {
        return comunicacionRepository.findById(id)
                .orElseThrow(() -> new AppException("Comunicacion no encontrada", HttpStatus.NOT_FOUND));
    }

    private ComunicacionResponse mapear(Comunicacion c) {
        return mapearConNombre(c, null);
    }

    private ComunicacionResponse mapearConNombre(Comunicacion c, String ejecutivoNombre) {
        return new ComunicacionResponse(
                c.getId(),
                c.getEntidadTipo().name(),
                c.getEntidadId(),
                c.getTipo(),
                c.getFecha(),
                c.getDescripcion(),
                c.getResultado(),
                c.getEjecutivoId(),
                ejecutivoNombre,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
