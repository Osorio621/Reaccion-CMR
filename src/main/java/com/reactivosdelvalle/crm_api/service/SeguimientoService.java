package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.SeguimientoRequest;
import com.reactivosdelvalle.crm_api.dto.response.SeguimientoResponse;
import com.reactivosdelvalle.crm_api.entity.Catalogo;
import com.reactivosdelvalle.crm_api.entity.Cliente;
import com.reactivosdelvalle.crm_api.entity.EstadoSeguimiento;
import com.reactivosdelvalle.crm_api.entity.Oportunidad;
import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.Seguimiento;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.SeguimientoMapper;
import com.reactivosdelvalle.crm_api.repository.CatalogoRepository;
import com.reactivosdelvalle.crm_api.repository.ClienteRepository;
import com.reactivosdelvalle.crm_api.repository.OportunidadRepository;
import com.reactivosdelvalle.crm_api.repository.SeguimientoRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
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
public class SeguimientoService {

    private final SeguimientoRepository seguimientoRepository;
    private final OportunidadRepository oportunidadRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoRepository catalogoRepository;
    private final SecurityUtils securityUtils;
    private final SeguimientoMapper seguimientoMapper;

    @Autowired
    public SeguimientoService(
            SeguimientoRepository seguimientoRepository,
            OportunidadRepository oportunidadRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            CatalogoRepository catalogoRepository,
            SecurityUtils securityUtils,
            SeguimientoMapper seguimientoMapper) {
        this.seguimientoRepository = seguimientoRepository;
        this.oportunidadRepository = oportunidadRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoRepository = catalogoRepository;
        this.securityUtils = securityUtils;
        this.seguimientoMapper = seguimientoMapper;
    }

    @Transactional(readOnly = true)
    public List<SeguimientoResponse> listar() {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        List<Seguimiento> lista;

        if (usuario != null && usuario.getRol() == RolUsuario.EJECUTIVO) {
            lista = seguimientoRepository.findByEjecutivoIdOrderByFechaProgramadaDesc(usuario.getId());
        } else {
            lista = seguimientoRepository.findAllByOrderByFechaProgramadaDesc();
        }

        return mapear(lista);
    }

    @Transactional(readOnly = true)
    public List<SeguimientoResponse> listarVencidos() {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        List<Seguimiento> lista;
        LocalDate hoy = LocalDate.now();

        if (usuario != null && usuario.getRol() == RolUsuario.EJECUTIVO) {
            lista = seguimientoRepository.findByEjecutivoIdAndEstadoAndFechaProgramadaBefore(
                    usuario.getId(), EstadoSeguimiento.PENDIENTE, hoy);
        } else {
            lista = seguimientoRepository.findByEstadoAndFechaProgramadaBefore(EstadoSeguimiento.PENDIENTE, hoy);
        }

        return mapear(lista);
    }

    @Transactional(readOnly = true)
    public SeguimientoResponse ver(Long id) {
        Seguimiento seg = getSeguimiento(id);
        verificarAcceso(seg);
        return mapear(List.of(seg)).get(0);
    }

    @Transactional
    public SeguimientoResponse crear(SeguimientoRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        validarReglasDeNegocio(request);

        EstadoSeguimiento estado = request.getEstado() != null ? request.getEstado() : EstadoSeguimiento.PENDIENTE;

        Seguimiento seg = Seguimiento.builder()
                .oportunidadId(request.getOportunidadId())
                .ejecutivoId(usuario.getId())
                .tipo(request.getTipo())
                .fechaProgramada(request.getFechaProgramada())
                .fechaRealizada(request.getFechaRealizada())
                .estado(estado)
                .notas(request.getNotas())
                .proximaAccion(request.getProximaAccion())
                .build();

        return mapear(List.of(seguimientoRepository.save(seg))).get(0);
    }

    @Transactional
    public SeguimientoResponse actualizar(Long id, SeguimientoRequest request) {
        Seguimiento seg = getSeguimiento(id);
        verificarAcceso(seg);
        validarReglasDeNegocio(request);

        seg.setOportunidadId(request.getOportunidadId());
        seg.setTipo(request.getTipo());
        seg.setFechaProgramada(request.getFechaProgramada());
        seg.setFechaRealizada(request.getFechaRealizada());
        if (request.getEstado() != null) {
            seg.setEstado(request.getEstado());
        }
        seg.setNotas(request.getNotas());
        seg.setProximaAccion(request.getProximaAccion());

        // Si se marca como COMPLETADO y no se indica fecha realizada, se coloca el día de hoy
        if (seg.getEstado() == EstadoSeguimiento.COMPLETADO && seg.getFechaRealizada() == null) {
            seg.setFechaRealizada(LocalDate.now());
        }

        return mapear(List.of(seguimientoRepository.save(seg))).get(0);
    }

    @Transactional
    public SeguimientoResponse cerrar(Long id, String notas, String proximaAccion) {
        Seguimiento seg = getSeguimiento(id);
        verificarAcceso(seg);

        seg.setEstado(EstadoSeguimiento.COMPLETADO);
        seg.setFechaRealizada(LocalDate.now());
        if (notas != null) seg.setNotas(notas);
        if (proximaAccion != null) seg.setProximaAccion(proximaAccion);

        return mapear(List.of(seguimientoRepository.save(seg))).get(0);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!securityUtils.esGerenteOAdmin()) {
            throw new AppException("Solo un Gerente o Administrador puede eliminar seguimientos", HttpStatus.FORBIDDEN);
        }
        Seguimiento seg = getSeguimiento(id);
        seguimientoRepository.delete(seg);
    }

    private Seguimiento getSeguimiento(Long id) {
        return seguimientoRepository.findById(id)
                .orElseThrow(() -> new AppException("Seguimiento no encontrado con id: " + id, HttpStatus.NOT_FOUND));
    }

    private void verificarAcceso(Seguimiento seg) {
        if (!securityUtils.puedeAccederA(seg.getEjecutivoId())) {
            throw new AppException("Este registro pertenece a otro ejecutivo", HttpStatus.FORBIDDEN);
        }
    }

    private void validarReglasDeNegocio(SeguimientoRequest request) {
        // Validar que la oportunidad existe y esté activa
        Oportunidad op = oportunidadRepository.findByIdAndActivoTrue(request.getOportunidadId())
                .orElseThrow(() -> new AppException("La oportunidad indicada no existe o está inactiva", HttpStatus.BAD_REQUEST));

        // Validar tipo de seguimiento contra el catálogo TIPO_SEGUIMIENTO
        catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("TIPO_SEGUIMIENTO").stream()
                .filter(c -> c.getCodigo().equals(request.getTipo()) || c.getNombre().equals(request.getTipo()))
                .findFirst()
                .orElseThrow(() -> new AppException("El tipo de seguimiento '" + request.getTipo() + "' no es válido en el catálogo", HttpStatus.BAD_REQUEST));
    }

    /**
     * Mapea los seguimientos resolviendo todos los nombres en consultas por lotes
     * (una por catálogo) en lugar de una consulta por cada relación (N+1).
     */
    private List<SeguimientoResponse> mapear(List<Seguimiento> seguimientos) {
        Set<Long> oportunidadIds = new HashSet<>();
        Set<Long> usuarioIds = new HashSet<>();

        for (Seguimiento s : seguimientos) {
            oportunidadIds.add(s.getOportunidadId());
            usuarioIds.add(s.getEjecutivoId());
        }

        Map<Long, Oportunidad> oportunidades = oportunidadIds.isEmpty() ? Map.of()
                : oportunidadRepository.findAllById(oportunidadIds).stream()
                        .collect(Collectors.toMap(Oportunidad::getId, o -> o));

        Set<Long> clienteIds = oportunidades.values().stream()
                .map(Oportunidad::getClienteId)
                .collect(Collectors.toSet());

        Map<Long, String> clientesNombres = clienteIds.isEmpty() ? Map.of()
                : clienteRepository.findAllById(clienteIds).stream()
                        .collect(Collectors.toMap(Cliente::getId, Cliente::getNombre));

        Map<Long, String> oportunidadesNombres = oportunidades.values().stream()
                .collect(Collectors.toMap(Oportunidad::getId, Oportunidad::getNombre));

        Map<Long, String> clienteNombresPorOportunidad = oportunidades.values().stream()
                .filter(o -> clientesNombres.containsKey(o.getClienteId()))
                .collect(Collectors.toMap(Oportunidad::getId, o -> clientesNombres.get(o.getClienteId())));

        Map<Long, String> ejecutivosNombres = usuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u.getNombre() + " " + u.getApellido()));

        Map<String, String> tiposNombres = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc("TIPO_SEGUIMIENTO").stream()
                .collect(Collectors.toMap(Catalogo::getCodigo, Catalogo::getNombre));

        return seguimientos.stream()
                .map(s -> seguimientoMapper.toResponse(s, oportunidadesNombres,
                        clienteNombresPorOportunidad, ejecutivosNombres, tiposNombres))
                .toList();
    }
}
