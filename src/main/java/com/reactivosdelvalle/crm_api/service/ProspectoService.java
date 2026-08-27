package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.ConvertirProspectoRequest;
import com.reactivosdelvalle.crm_api.dto.request.OportunidadRequest;
import com.reactivosdelvalle.crm_api.dto.request.ProspectoRequest;
import com.reactivosdelvalle.crm_api.dto.response.ClienteResponse;
import com.reactivosdelvalle.crm_api.dto.response.ConvertirProspectoResponse;
import com.reactivosdelvalle.crm_api.dto.response.ProspectoResponse;
import com.reactivosdelvalle.crm_api.entity.Cliente;
import com.reactivosdelvalle.crm_api.entity.Prospecto;
import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.ClienteMapper;
import com.reactivosdelvalle.crm_api.mapper.ProspectoMapper;
import com.reactivosdelvalle.crm_api.repository.CatalogoRepository;
import com.reactivosdelvalle.crm_api.repository.ClienteRepository;
import com.reactivosdelvalle.crm_api.repository.ProspectoRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProspectoService {

    private final ProspectoRepository prospectoRepository;
    private final ClienteRepository clienteRepository;
    private final CatalogoRepository catalogoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;
    private final ProspectoMapper prospectoMapper;
    private final ClienteMapper clienteMapper;
    private final OportunidadService oportunidadService;

    @Autowired
    public ProspectoService(
            ProspectoRepository prospectoRepository,
            ClienteRepository clienteRepository,
            CatalogoRepository catalogoRepository,
            UsuarioRepository usuarioRepository,
            SecurityUtils securityUtils,
            ProspectoMapper prospectoMapper,
            ClienteMapper clienteMapper,
            OportunidadService oportunidadService) {
        this.prospectoRepository = prospectoRepository;
        this.clienteRepository = clienteRepository;
        this.catalogoRepository = catalogoRepository;
        this.usuarioRepository = usuarioRepository;
        this.securityUtils = securityUtils;
        this.prospectoMapper = prospectoMapper;
        this.clienteMapper = clienteMapper;
        this.oportunidadService = oportunidadService;
    }

    @Transactional(readOnly = true)
    public List<ProspectoResponse> findAll() {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        List<Prospecto> prospectos;
        if (usuario != null && usuario.getRol() == RolUsuario.EJECUTIVO) {
            prospectos = prospectoRepository.findByResponsableIdAndActivoTrueOrderByFechaProximaAccionAsc(usuario.getId());
        } else {
            prospectos = prospectoRepository.findAllByActivoTrueOrderByFechaProximaAccionAsc();
        }
        return prospectos.stream().map(prospectoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProspectoResponse findById(Long id) {
        Prospecto prospecto = getActivo(id);
        verificarAcceso(prospecto);
        return prospectoMapper.toResponse(prospecto);
    }

    @Transactional
    public ProspectoResponse create(ProspectoRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();

        Long responsableId = usuario.getId();
        if (securityUtils.esGerenteOAdmin() && request.getResponsableId() != null) {
            responsableId = request.getResponsableId();
        }
        verificarUsuarioActivo(responsableId);

        validarEtapaProspecto(request.getEtapaId());
        validarCatalogos(request.getTipoId(), request.getIndustriaId(), request.getZonaId());

        Prospecto prospecto = Prospecto.builder()
                .nombre(request.getNombre())
                .empresa(request.getEmpresa())
                .tipoId(request.getTipoId())
                .industriaId(request.getIndustriaId())
                .zonaId(request.getZonaId())
                .responsableId(responsableId)
                .etapaId(request.getEtapaId())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .sitioWeb(request.getSitioWeb())
                .notas(request.getNotas())
                .proximaAccion(request.getProximaAccion())
                .fechaProximaAccion(request.getFechaProximaAccion())
                .createdById(usuario.getId())
                .build();

        return prospectoMapper.toResponse(prospectoRepository.save(prospecto));
    }

    @Transactional
    public ProspectoResponse update(Long id, ProspectoRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        Prospecto prospecto = getActivo(id);
        verificarAcceso(prospecto);

        validarEtapaProspecto(request.getEtapaId());
        validarCatalogos(request.getTipoId(), request.getIndustriaId(), request.getZonaId());

        prospecto.setNombre(request.getNombre());
        prospecto.setEmpresa(request.getEmpresa());
        prospecto.setTipoId(request.getTipoId());
        prospecto.setIndustriaId(request.getIndustriaId());
        prospecto.setZonaId(request.getZonaId());
        if (securityUtils.esGerenteOAdmin() && request.getResponsableId() != null) {
            verificarUsuarioActivo(request.getResponsableId());
            prospecto.setResponsableId(request.getResponsableId());
        }
        prospecto.setEtapaId(request.getEtapaId());
        prospecto.setTelefono(request.getTelefono());
        prospecto.setEmail(request.getEmail());
        prospecto.setSitioWeb(request.getSitioWeb());
        prospecto.setNotas(request.getNotas());
        prospecto.setProximaAccion(request.getProximaAccion());
        prospecto.setFechaProximaAccion(request.getFechaProximaAccion());

        return prospectoMapper.toResponse(prospectoRepository.save(prospecto));
    }

    @Transactional
    public void delete(Long id) {
        Prospecto prospecto = getActivo(id);
        prospecto.setActivo(false);
        prospectoRepository.save(prospecto);
    }

    @Transactional
    public ConvertirProspectoResponse convertir(Long id, ConvertirProspectoRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        Prospecto prospecto = getActivo(id);
        verificarAcceso(prospecto);

        if (Boolean.TRUE.equals(prospecto.getConvertido())) {
            throw new AppException("El prospecto ya fue convertido a cliente", HttpStatus.CONFLICT, "PROSPECTO_CONVERTIDO");
        }

        Long tipoId = request.getTipoId() != null ? request.getTipoId() : prospecto.getTipoId();
        Long industriaId = request.getIndustriaId() != null ? request.getIndustriaId() : prospecto.getIndustriaId();
        Long zonaId = request.getZonaId() != null ? request.getZonaId() : prospecto.getZonaId();
        validarCatalogos(tipoId, industriaId, zonaId);

        Cliente cliente = Cliente.builder()
                .nombre(prospecto.getNombre())
                .razonSocial(request.getRazonSocial())
                .rfc(request.getRfc())
                .tipoId(tipoId)
                .industriaId(industriaId)
                .zonaId(zonaId)
                .ejecutivoId(prospecto.getResponsableId())
                .telefonoPrincipal(request.getTelefonoPrincipal() != null ? request.getTelefonoPrincipal() : prospecto.getTelefono())
                .emailPrincipal(request.getEmailPrincipal() != null ? request.getEmailPrincipal() : prospecto.getEmail())
                .sitioWeb(request.getSitioWeb() != null ? request.getSitioWeb() : prospecto.getSitioWeb())
                .direccion(request.getDireccion())
                .ciudad(request.getCiudad())
                .estadoRegion(request.getEstadoRegion())
                .notas(request.getNotas() != null ? request.getNotas() : prospecto.getNotas())
                .fechaPrimeraCompra(request.getFechaPrimeraCompra())
                .createdById(usuario.getId())
                .updatedById(usuario.getId())
                .build();

        Cliente guardado = clienteRepository.save(cliente);

        prospecto.setConvertido(true);
        prospecto.setClienteId(guardado.getId());
        prospectoRepository.save(prospecto);

        // Oportunidad inicial opcional: se crea en esta misma transacción con
        // todas las validaciones de Regla de Oro del OportunidadService
        var oportunidadCreada = java.util.Optional.ofNullable(request.getOportunidad())
                .map(bloque -> construirOportunidadRequest(bloque, prospecto))
                .map(oportunidadService::create)
                .orElse(null);

        return ConvertirProspectoResponse.builder()
                .cliente(clienteMapper.toResponse(guardado))
                .oportunidad(oportunidadCreada)
                .build();
    }

    private OportunidadRequest construirOportunidadRequest(
            ConvertirProspectoRequest.OportunidadInicial bloque, Prospecto prospecto) {
        OportunidadRequest request = new OportunidadRequest();
        request.setNombre(bloque.getNombre());
        // El cliente y el vínculo con el prospecto los define la conversión
        request.setClienteId(prospecto.getClienteId());
        request.setProspectoId(prospecto.getId());
        // El responsable del negocio es quien traía el prospecto
        request.setEjecutivoId(prospecto.getResponsableId());
        request.setEtapaId(bloque.getEtapaId());
        request.setValor(bloque.getValor());
        request.setProbabilidad(bloque.getProbabilidad());
        request.setFechaEstimadaCierre(bloque.getFechaEstimadaCierre());
        request.setProximaAccion(bloque.getProximaAccion());
        request.setFechaProximaAccion(bloque.getFechaProximaAccion());
        request.setDescripcion(bloque.getDescripcion());
        request.setCompetencia(bloque.getCompetencia());
        return request;
    }

    private Prospecto getActivo(Long id) {
        return prospectoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new AppException("Prospecto no encontrado con id: " + id, HttpStatus.NOT_FOUND));
    }

    private void verificarAcceso(Prospecto prospecto) {
        if (!securityUtils.puedeAccederA(prospecto.getResponsableId())) {
            throw new AppException("Este registro pertenece a otro ejecutivo", HttpStatus.FORBIDDEN);
        }
    }

    private void verificarUsuarioActivo(Long responsableId) {
        usuarioRepository.findById(responsableId)
                .filter(Usuario::getActivo)
                .orElseThrow(() -> new AppException("El usuario indicado no existe o está inactivo", HttpStatus.BAD_REQUEST));
    }

    private void validarEtapaProspecto(Long etapaId) {
        catalogoRepository.findByIdAndActivoTrue(etapaId)
                .filter(c -> c.getTipo().equals("ETAPA_PROSPECTO"))
                .orElseThrow(() -> new AppException(
                        "La etapa debe ser un catálogo activo del tipo ETAPA_PROSPECTO (Regla 5)",
                        HttpStatus.BAD_REQUEST, "REGLA_5_INCUMPLIDA"));
    }

    private void validarCatalogos(Long tipoId, Long industriaId, Long zonaId) {
        if (tipoId != null) validarCatalogo(tipoId, "TIPO_CLIENTE");
        if (industriaId != null) validarCatalogo(industriaId, "INDUSTRIA");
        if (zonaId != null) validarCatalogo(zonaId, "ZONA_GEOGRAFICA");
    }

    private void validarCatalogo(Long id, String tipoEsperado) {
        catalogoRepository.findByIdAndActivoTrue(id)
                .filter(c -> c.getTipo().equals(tipoEsperado))
                .orElseThrow(() -> new AppException(
                        "El catálogo " + id + " no existe o no es del tipo " + tipoEsperado,
                        HttpStatus.BAD_REQUEST));
    }
}