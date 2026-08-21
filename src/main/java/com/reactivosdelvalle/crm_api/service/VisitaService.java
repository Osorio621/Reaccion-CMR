package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.VisitaRequest;
import com.reactivosdelvalle.crm_api.dto.response.VisitaResponse;
import com.reactivosdelvalle.crm_api.entity.*;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.VisitaMapper;
import com.reactivosdelvalle.crm_api.repository.*;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VisitaService {

    private final VisitaRepository visitaRepository;
    private final ClienteRepository clienteRepository;
    private final ProspectoRepository prospectoRepository;
    private final OportunidadRepository oportunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoRepository catalogoRepository;
    private final SecurityUtils securityUtils;
    private final VisitaMapper visitaMapper;

    @Autowired
    public VisitaService(
            VisitaRepository visitaRepository,
            ClienteRepository clienteRepository,
            ProspectoRepository prospectoRepository,
            OportunidadRepository oportunidadRepository,
            UsuarioRepository usuarioRepository,
            CatalogoRepository catalogoRepository,
            SecurityUtils securityUtils,
            VisitaMapper visitaMapper) {
        this.visitaRepository = visitaRepository;
        this.clienteRepository = clienteRepository;
        this.prospectoRepository = prospectoRepository;
        this.oportunidadRepository = oportunidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoRepository = catalogoRepository;
        this.securityUtils = securityUtils;
        this.visitaMapper = visitaMapper;
    }

    @Transactional(readOnly = true)
    public List<VisitaResponse> listar() {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        List<Visita> visitas;

        if (usuario != null && usuario.getRol() == RolUsuario.EJECUTIVO) {
            visitas = visitaRepository.findByEjecutivoIdOrderByFechaDesc(usuario.getId());
        } else {
            visitas = visitaRepository.findAllByOrderByFechaDesc();
        }

        return mapear(visitas);
    }

    @Transactional(readOnly = true)
    public VisitaResponse ver(Long id) {
        Visita visita = getVisita(id);
        verificarAcceso(visita);
        return mapear(List.of(visita)).get(0);
    }

    @Transactional
    public VisitaResponse crear(VisitaRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        
        validarReglasDeNegocio(request);

        Visita visita = Visita.builder()
                .tipoEntidad(request.getTipoEntidad())
                .clienteId(request.getClienteId())
                .prospectoId(request.getProspectoId())
                .oportunidadId(request.getOportunidadId())
                .ejecutivoId(usuario.getId())
                .fecha(request.getFecha())
                .objetivo(request.getObjetivo())
                .necesidadDetectada(request.getNecesidadDetectada())
                .competenciaMencionada(request.getCompetenciaMencionada())
                .resultadoId(request.getResultadoId())
                .oportunidadGenerada(request.getOportunidadGenerada())
                .compromiso(request.getCompromiso())
                .notasAdicionales(request.getNotasAdicionales())
                .build();

        return mapear(List.of(visitaRepository.save(visita))).get(0);
    }

    @Transactional
    public VisitaResponse actualizar(Long id, VisitaRequest request) {
        Visita visita = getVisita(id);
        verificarAcceso(visita);
        validarReglasDeNegocio(request);

        visita.setTipoEntidad(request.getTipoEntidad());
        visita.setClienteId(request.getClienteId());
        visita.setProspectoId(request.getProspectoId());
        visita.setOportunidadId(request.getOportunidadId());
        visita.setFecha(request.getFecha());
        visita.setObjetivo(request.getObjetivo());
        visita.setNecesidadDetectada(request.getNecesidadDetectada());
        visita.setCompetenciaMencionada(request.getCompetenciaMencionada());
        visita.setResultadoId(request.getResultadoId());
        visita.setOportunidadGenerada(request.getOportunidadGenerada());
        visita.setCompromiso(request.getCompromiso());
        visita.setNotasAdicionales(request.getNotasAdicionales());

        return mapear(List.of(visitaRepository.save(visita))).get(0);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!securityUtils.esGerenteOAdmin()) {
            throw new AppException("Solo un Gerente o Administrador puede eliminar visitas", HttpStatus.FORBIDDEN);
        }
        Visita visita = getVisita(id);
        visitaRepository.delete(visita);
    }

    private Visita getVisita(Long id) {
        return visitaRepository.findById(id)
                .orElseThrow(() -> new AppException("Visita no encontrada con id: " + id, HttpStatus.NOT_FOUND));
    }

    private void verificarAcceso(Visita visita) {
        if (!securityUtils.puedeAccederA(visita.getEjecutivoId())) {
            throw new AppException("Este registro pertenece a otro ejecutivo", HttpStatus.FORBIDDEN);
        }
    }

    private void validarReglasDeNegocio(VisitaRequest request) {
        // Regla 2: Visitas registradas máximo 24 horas después
        LocalDate hoy = LocalDate.now();
        if (request.getFecha().isBefore(hoy.minusDays(1))) {
            throw new AppException("Las visitas deben registrarse como máximo 24 horas después del día de la visita (fecha >= ayer)",
                    HttpStatus.BAD_REQUEST, "FECHA_VISITA_INVALIDA");
        }

        // Validación de relaciones exclusivas (cliente o prospecto)
        if (request.getTipoEntidad() == TipoEntidadVisita.CLIENTE) {
            if (request.getClienteId() == null) {
                throw new AppException("Debe proporcionar un clienteId si el tipo de entidad es CLIENTE", HttpStatus.BAD_REQUEST);
            }
            if (request.getProspectoId() != null) {
                throw new AppException("No debe proporcionar un prospectoId si el tipo de entidad es CLIENTE", HttpStatus.BAD_REQUEST);
            }
            clienteRepository.findByIdAndActivoTrue(request.getClienteId())
                    .orElseThrow(() -> new AppException("El cliente indicado no existe o está inactivo", HttpStatus.BAD_REQUEST));
        } else if (request.getTipoEntidad() == TipoEntidadVisita.PROSPECTO) {
            if (request.getProspectoId() == null) {
                throw new AppException("Debe proporcionar un prospectoId si el tipo de entidad es PROSPECTO", HttpStatus.BAD_REQUEST);
            }
            if (request.getClienteId() != null) {
                throw new AppException("No debe proporcionar un clienteId si el tipo de entidad es PROSPECTO", HttpStatus.BAD_REQUEST);
            }
            prospectoRepository.findByIdAndActivoTrue(request.getProspectoId())
                    .orElseThrow(() -> new AppException("El prospecto indicado no existe o está inactivo", HttpStatus.BAD_REQUEST));
        }

        // Validación de Oportunidad (si se indica)
        if (request.getOportunidadId() != null) {
            oportunidadRepository.findByIdAndActivoTrue(request.getOportunidadId())
                    .orElseThrow(() -> new AppException("La oportunidad indicada no existe o está inactiva", HttpStatus.BAD_REQUEST));
        }

        // Validación del catálogo de resultado (tipo RESULTADO_VISITA)
        catalogoRepository.findByIdAndActivoTrue(request.getResultadoId())
                .filter(c -> "RESULTADO_VISITA".equals(c.getTipo()))
                .orElseThrow(() -> new AppException("El resultado indicado no existe o no es de tipo RESULTADO_VISITA", HttpStatus.BAD_REQUEST));
    }

    /**
     * Mapea las visitas resolviendo todos los nombres en consultas por lotes
     * (una por catálogo) en lugar de una consulta por cada relación (N+1).
     */
    private List<VisitaResponse> mapear(List<Visita> visitas) {
        Set<Long> clienteIds = new HashSet<>();
        Set<Long> prospectoIds = new HashSet<>();
        Set<Long> oportunidadIds = new HashSet<>();
        Set<Long> usuarioIds = new HashSet<>();
        Set<Long> resultadoIds = new HashSet<>();

        for (Visita v : visitas) {
            if (v.getClienteId() != null) clienteIds.add(v.getClienteId());
            if (v.getProspectoId() != null) prospectoIds.add(v.getProspectoId());
            if (v.getOportunidadId() != null) oportunidadIds.add(v.getOportunidadId());
            usuarioIds.add(v.getEjecutivoId());
            resultadoIds.add(v.getResultadoId());
        }

        Map<Long, String> clientesNombres = clienteIds.isEmpty() ? Map.of()
                : clienteRepository.findAllById(clienteIds).stream()
                        .collect(Collectors.toMap(Cliente::getId, Cliente::getNombre));

        Map<Long, String> prospectosNombres = prospectoIds.isEmpty() ? Map.of()
                : prospectoRepository.findAllById(prospectoIds).stream()
                        .collect(Collectors.toMap(Prospecto::getId, Prospecto::getNombre));

        Map<Long, String> oportunidadesNombres = oportunidadIds.isEmpty() ? Map.of()
                : oportunidadRepository.findAllById(oportunidadIds).stream()
                        .collect(Collectors.toMap(Oportunidad::getId, Oportunidad::getNombre));

        Map<Long, String> ejecutivosNombres = usuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u.getNombre() + " " + u.getApellido()));

        Map<Long, String> resultadosNombres = catalogoRepository.findAllById(resultadoIds).stream()
                .collect(Collectors.toMap(Catalogo::getId, Catalogo::getNombre));

        return visitas.stream()
                .map(v -> visitaMapper.toResponse(v, clientesNombres, prospectosNombres,
                        oportunidadesNombres, ejecutivosNombres, resultadosNombres))
                .toList();
    }
}
