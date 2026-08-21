package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.CambioEtapaRequest;
import com.reactivosdelvalle.crm_api.dto.request.CerrarOportunidadRequest;
import com.reactivosdelvalle.crm_api.dto.request.OportunidadRequest;
import com.reactivosdelvalle.crm_api.dto.response.OportunidadEtapaHistResponse;
import com.reactivosdelvalle.crm_api.dto.response.OportunidadResponse;
import com.reactivosdelvalle.crm_api.dto.response.PipelineEtapaResponse;
import com.reactivosdelvalle.crm_api.entity.*;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.OportunidadEtapaHistMapper;
import com.reactivosdelvalle.crm_api.mapper.OportunidadMapper;
import com.reactivosdelvalle.crm_api.repository.*;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OportunidadService {

    private final OportunidadRepository oportunidadRepository;
    private final OportunidadEtapaHistRepository etapaHistRepository;
    private final ClienteRepository clienteRepository;
    private final ProspectoRepository prospectoRepository;
    private final CatalogoRepository catalogoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;
    private final EntityManager entityManager;
    private final OportunidadMapper oportunidadMapper;
    private final OportunidadEtapaHistMapper etapaHistMapper;

    @Autowired
    public OportunidadService(
            OportunidadRepository oportunidadRepository,
            OportunidadEtapaHistRepository etapaHistRepository,
            ClienteRepository clienteRepository,
            ProspectoRepository prospectoRepository,
            CatalogoRepository catalogoRepository,
            UsuarioRepository usuarioRepository,
            SecurityUtils securityUtils,
            EntityManager entityManager,
            OportunidadMapper oportunidadMapper,
            OportunidadEtapaHistMapper etapaHistMapper) {
        this.oportunidadRepository = oportunidadRepository;
        this.etapaHistRepository = etapaHistRepository;
        this.clienteRepository = clienteRepository;
        this.prospectoRepository = prospectoRepository;
        this.catalogoRepository = catalogoRepository;
        this.usuarioRepository = usuarioRepository;
        this.securityUtils = securityUtils;
        this.entityManager = entityManager;
        this.oportunidadMapper = oportunidadMapper;
        this.etapaHistMapper = etapaHistMapper;
    }

    @Transactional(readOnly = true)
    public List<OportunidadResponse> findAll() {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        List<Oportunidad> oportunidades;
        if (usuario != null && usuario.getRol() == RolUsuario.EJECUTIVO) {
            oportunidades = oportunidadRepository.findByEjecutivoIdAndActivoTrueOrderByUpdatedAtDesc(usuario.getId());
        } else {
            oportunidades = oportunidadRepository.findAllByActivoTrueOrderByUpdatedAtDesc();
        }
        return oportunidades.stream().map(oportunidadMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OportunidadResponse findById(Long id) {
        Oportunidad oportunidad = getActivo(id);
        verificarAcceso(oportunidad);
        return oportunidadMapper.toResponse(oportunidad);
    }

    @Transactional
    public OportunidadResponse create(OportunidadRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();

        Long ejecutivoId = usuario.getId();
        if (securityUtils.esGerenteOAdmin() && request.getEjecutivoId() != null) {
            ejecutivoId = request.getEjecutivoId();
        }
        verificarUsuarioActivo(ejecutivoId);

        validarReglaDeOro(request);
        validarEtapaPipeline(request.getEtapaId());
        verificarClienteActivo(request.getClienteId());
        if (request.getProspectoId() != null) {
            verificarProspectoActivo(request.getProspectoId());
        }

        Oportunidad oportunidad = Oportunidad.builder()
                .nombre(request.getNombre())
                .clienteId(request.getClienteId())
                .prospectoId(request.getProspectoId())
                .ejecutivoId(ejecutivoId)
                .etapaId(request.getEtapaId())
                .valor(request.getValor())
                .probabilidad(request.getProbabilidad())
                .fechaEstimadaCierre(request.getFechaEstimadaCierre())
                .proximaAccion(request.getProximaAccion())
                .fechaProximaAccion(request.getFechaProximaAccion())
                .descripcion(request.getDescripcion())
                .competencia(request.getCompetencia())
                .createdById(usuario.getId())
                .updatedById(usuario.getId())
                .build();

        Oportunidad guardada = oportunidadRepository.save(oportunidad);
        entityManager.flush();
        entityManager.refresh(guardada);
        registrarHistorial(guardada.getId(), null, guardada.getEtapaId(), usuario.getId(), null);

        return oportunidadMapper.toResponse(guardada);
    }

    @Transactional
    public OportunidadResponse update(Long id, OportunidadRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        Oportunidad oportunidad = getActivo(id);
        verificarAcceso(oportunidad);

        validarReglaDeOro(request);
        validarEtapaPipeline(request.getEtapaId());
        verificarClienteActivo(request.getClienteId());
        if (request.getProspectoId() != null) {
            verificarProspectoActivo(request.getProspectoId());
        }

        boolean cambioEtapa = !oportunidad.getEtapaId().equals(request.getEtapaId());
        Long etapaAnterior = oportunidad.getEtapaId();

        oportunidad.setNombre(request.getNombre());
        oportunidad.setClienteId(request.getClienteId());
        oportunidad.setProspectoId(request.getProspectoId());
        if (securityUtils.esGerenteOAdmin() && request.getEjecutivoId() != null) {
            verificarUsuarioActivo(request.getEjecutivoId());
            oportunidad.setEjecutivoId(request.getEjecutivoId());
        }
        oportunidad.setEtapaId(request.getEtapaId());
        oportunidad.setValor(request.getValor());
        oportunidad.setProbabilidad(request.getProbabilidad());
        oportunidad.setFechaEstimadaCierre(request.getFechaEstimadaCierre());
        oportunidad.setProximaAccion(request.getProximaAccion());
        oportunidad.setFechaProximaAccion(request.getFechaProximaAccion());
        oportunidad.setDescripcion(request.getDescripcion());
        oportunidad.setCompetencia(request.getCompetencia());
        oportunidad.setUpdatedById(usuario.getId());

        Oportunidad guardada = oportunidadRepository.save(oportunidad);
        entityManager.flush();
        entityManager.refresh(guardada);

        if (cambioEtapa) {
            registrarHistorial(guardada.getId(), etapaAnterior, guardada.getEtapaId(), usuario.getId(), null);
        }

        return oportunidadMapper.toResponse(guardada);
    }

    @Transactional
    public OportunidadResponse cambiarEtapa(Long id, CambioEtapaRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        Oportunidad oportunidad = getActivo(id);
        verificarAcceso(oportunidad);
        verificarActiva(oportunidad);

        if (oportunidad.getEtapaId().equals(request.getEtapaNuevaId())) {
            throw new AppException("La oportunidad ya se encuentra en la etapa indicada", HttpStatus.BAD_REQUEST);
        }
        validarEtapaPipeline(request.getEtapaNuevaId());

        Long etapaAnterior = oportunidad.getEtapaId();
        oportunidad.setEtapaId(request.getEtapaNuevaId());
        oportunidad.setUpdatedById(usuario.getId());

        Oportunidad guardada = oportunidadRepository.save(oportunidad);
        registrarHistorial(guardada.getId(), etapaAnterior, guardada.getEtapaId(), usuario.getId(), request.getNotas());

        return oportunidadMapper.toResponse(guardada);
    }

    @Transactional
    public OportunidadResponse cerrar(Long id, CerrarOportunidadRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        Oportunidad oportunidad = getActivo(id);
        verificarAcceso(oportunidad);
        verificarActiva(oportunidad);

        if (request.getEstado() != EstadoOportunidad.GANADA
                && request.getEstado() != EstadoOportunidad.PERDIDA
                && request.getEstado() != EstadoOportunidad.CONGELADA) {
            throw new AppException("El estado de cierre debe ser GANADA, PERDIDA o CONGELADA", HttpStatus.BAD_REQUEST);
        }
        if (request.getEstado() == EstadoOportunidad.PERDIDA
                && (request.getMotivoPerdida() == null || request.getMotivoPerdida().isBlank())) {
            throw new AppException("El motivo de pérdida es obligatorio cuando el estado es PERDIDA", HttpStatus.BAD_REQUEST);
        }

        oportunidad.setEstado(request.getEstado());
        oportunidad.setMotivoPerdida(request.getMotivoPerdida());
        oportunidad.setFechaCierreReal(request.getFechaCierreReal() != null ? request.getFechaCierreReal() : LocalDate.now());
        oportunidad.setUpdatedById(usuario.getId());

        return oportunidadMapper.toResponse(oportunidadRepository.save(oportunidad));
    }

    @Transactional
    public void delete(Long id) {
        Oportunidad oportunidad = getActivo(id);
        oportunidad.setActivo(false);
        oportunidadRepository.save(oportunidad);
    }

    @Transactional(readOnly = true)
    public List<OportunidadEtapaHistResponse> historial(Long id) {
        verificarAcceso(getActivo(id));
        return etapaHistRepository.findByOportunidadIdOrderByCreatedAtDesc(id).stream()
                .map(etapaHistMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PipelineEtapaResponse> pipeline() {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        List<Oportunidad> oportunidades;
        if (usuario != null && usuario.getRol() == RolUsuario.EJECUTIVO) {
            oportunidades = oportunidadRepository.findByEjecutivoIdAndActivoTrueAndEstadoOrderByUpdatedAtDesc(usuario.getId(), EstadoOportunidad.ACTIVA);
        } else {
            oportunidades = oportunidadRepository.findAllByActivoTrueAndEstadoOrderByUpdatedAtDesc(EstadoOportunidad.ACTIVA);
        }

        Map<Long, List<Oportunidad>> porEtapa = oportunidades.stream()
                .collect(Collectors.groupingBy(Oportunidad::getEtapaId));

        List<Catalogo> etapas = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("ETAPA_PIPELINE");

        return etapas.stream().map(etapa -> {
            List<Oportunidad> lista = porEtapa.getOrDefault(etapa.getId(), List.of());
            BigDecimal total = lista.stream()
                    .map(o -> o.getValorPonderado() != null ? o.getValorPonderado() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return PipelineEtapaResponse.builder()
                    .etapaId(etapa.getId())
                    .etapaNombre(etapa.getNombre())
                    .cantidad((long) lista.size())
                    .valorPonderadoTotal(total)
                    .build();
        }).toList();
    }

    private void registrarHistorial(Long oportunidadId, Long etapaAnteriorId, Long etapaNuevaId, Long usuarioId, String notas) {
        OportunidadEtapaHist hist = OportunidadEtapaHist.builder()
                .oportunidadId(oportunidadId)
                .etapaAnteriorId(etapaAnteriorId)
                .etapaNuevaId(etapaNuevaId)
                .usuarioId(usuarioId)
                .notas(notas)
                .build();
        etapaHistRepository.save(hist);
    }

    private void validarReglaDeOro(OportunidadRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()
                || request.getClienteId() == null
                || request.getEtapaId() == null
                || request.getValor() == null
                || request.getProbabilidad() == null
                || request.getFechaEstimadaCierre() == null
                || request.getProximaAccion() == null || request.getProximaAccion().isBlank()) {
            throw new AppException("Regla de Oro: nombre, cliente, etapa, valor, probabilidad, fecha estimada de cierre y próxima acción son obligatorios",
                    HttpStatus.BAD_REQUEST, "REGLA_DE_ORO_INCUMPLIDA");
        }
    }

    private void validarEtapaPipeline(Long etapaId) {
        catalogoRepository.findByIdAndActivoTrue(etapaId)
                .filter(c -> c.getTipo().equals("ETAPA_PIPELINE"))
                .orElseThrow(() -> new AppException(
                        "La etapa debe ser un catálogo activo del tipo ETAPA_PIPELINE",
                        HttpStatus.BAD_REQUEST));
    }

    private void verificarClienteActivo(Long clienteId) {
        clienteRepository.findByIdAndActivoTrue(clienteId)
                .orElseThrow(() -> new AppException("El cliente indicado no existe o está inactivo", HttpStatus.BAD_REQUEST));
    }

    private void verificarProspectoActivo(Long prospectoId) {
        prospectoRepository.findByIdAndActivoTrue(prospectoId)
                .orElseThrow(() -> new AppException("El prospecto indicado no existe o está inactivo", HttpStatus.BAD_REQUEST));
    }

    private void verificarUsuarioActivo(Long ejecutivoId) {
        usuarioRepository.findById(ejecutivoId)
                .filter(Usuario::getActivo)
                .orElseThrow(() -> new AppException("El ejecutivo indicado no existe o está inactivo", HttpStatus.BAD_REQUEST));
    }

    private void verificarActiva(Oportunidad oportunidad) {
        if (oportunidad.getEstado() != EstadoOportunidad.ACTIVA) {
            throw new AppException("La oportunidad ya no está activa (estado: " + oportunidad.getEstado() + ")", HttpStatus.BAD_REQUEST);
        }
    }

    private Oportunidad getActivo(Long id) {
        return oportunidadRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new AppException("Oportunidad no encontrada con id: " + id, HttpStatus.NOT_FOUND));
    }

    private void verificarAcceso(Oportunidad oportunidad) {
        if (!securityUtils.puedeAccederA(oportunidad.getEjecutivoId())) {
            throw new AppException("Este registro pertenece a otro ejecutivo", HttpStatus.FORBIDDEN);
        }
    }
}