package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.VentaRequest;
import com.reactivosdelvalle.crm_api.dto.response.VentaResponse;
import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.entity.Venta;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.VentaMapper;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import com.reactivosdelvalle.crm_api.repository.VentaRepository;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;
    private final VentaMapper ventaMapper;

    @Autowired
    public VentaService(
            VentaRepository ventaRepository,
            UsuarioRepository usuarioRepository,
            SecurityUtils securityUtils,
            VentaMapper ventaMapper) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.securityUtils = securityUtils;
        this.ventaMapper = ventaMapper;
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        List<Venta> ventas;

        if (usuario != null && usuario.getRol() == RolUsuario.EJECUTIVO) {
            ventas = ventaRepository.findByEjecutivoIdOrderByAnioDescMesDesc(usuario.getId());
        } else {
            ventas = ventaRepository.findAllByOrderByAnioDescMesDesc();
        }

        return mapear(ventas);
    }

    @Transactional(readOnly = true)
    public VentaResponse ver(Long id) {
        Venta venta = getVenta(id);
        verificarAcceso(venta.getEjecutivoId());
        return mapear(List.of(venta)).get(0);
    }

    @Transactional
    public VentaResponse crear(VentaRequest request) {
        UsuarioPrincipal usuarioActual = securityUtils.getUsuarioActual();
        Long ejecutivoId = request.getEjecutivoId();

        // Si es ejecutivo, forzar a que sea él mismo
        if (usuarioActual.getRol() == RolUsuario.EJECUTIVO) {
            ejecutivoId = usuarioActual.getId();
        } else if (ejecutivoId == null) {
            ejecutivoId = usuarioActual.getId();
        }

        // Verificar ejecutivo activo
        Long finalEjecutivoId = ejecutivoId;
        usuarioRepository.findById(finalEjecutivoId)
                .filter(Usuario::getActivo)
                .orElseThrow(() -> new AppException("El ejecutivo indicado no existe o está inactivo", HttpStatus.BAD_REQUEST));

        // Validar regla de negocio de unicidad (ejecutivo, año, mes)
        Optional<Venta> existente = ventaRepository.findByEjecutivoIdAndAnioAndMes(ejecutivoId, request.getAnio(), request.getMes());
        if (existente.isPresent()) {
            throw new AppException("Ya existe un registro de ventas para el ejecutivo en el periodo indicado (Año: " + request.getAnio() + ", Mes: " + request.getMes() + ")", 
                    HttpStatus.CONFLICT, "VENTA_PERIODO_DUPLICADO");
        }

        BigDecimal ventaReal = request.getVentaReal() != null ? request.getVentaReal() : BigDecimal.ZERO;
        BigDecimal forecast = request.getForecast() != null ? request.getForecast() : BigDecimal.ZERO;

        Venta venta = Venta.builder()
                .ejecutivoId(ejecutivoId)
                .updatedById(usuarioActual.getId())
                .anio(request.getAnio())
                .mes(request.getMes())
                .meta(request.getMeta())
                .ventaReal(ventaReal)
                .forecast(forecast)
                .notas(request.getNotas())
                .build();

        return mapear(List.of(ventaRepository.save(venta))).get(0);
    }

    @Transactional
    public VentaResponse actualizar(Long id, VentaRequest request) {
        Venta venta = getVenta(id);
        
        // Verificar que el usuario tenga acceso a editar
        verificarAcceso(venta.getEjecutivoId());
        
        UsuarioPrincipal usuarioActual = securityUtils.getUsuarioActual();

        // Validar si intentan cambiar ejecutivo, año o mes y entra en conflicto
        Long ejecutivoId = request.getEjecutivoId() != null ? request.getEjecutivoId() : venta.getEjecutivoId();
        if (usuarioActual.getRol() == RolUsuario.EJECUTIVO) {
            ejecutivoId = venta.getEjecutivoId(); // Los ejecutivos no pueden cambiar de ejecutivo
        }

        if (!ejecutivoId.equals(venta.getEjecutivoId()) 
                || !request.getAnio().equals(venta.getAnio()) 
                || !request.getMes().equals(venta.getMes())) {
            
            // Verificar ejecutivo activo si cambió
            if (!ejecutivoId.equals(venta.getEjecutivoId())) {
                usuarioRepository.findById(ejecutivoId)
                        .filter(Usuario::getActivo)
                        .orElseThrow(() -> new AppException("El ejecutivo indicado no existe o está inactivo", HttpStatus.BAD_REQUEST));
            }

            Optional<Venta> existente = ventaRepository.findByEjecutivoIdAndAnioAndMes(ejecutivoId, request.getAnio(), request.getMes());
            if (existente.isPresent() && !existente.get().getId().equals(venta.getId())) {
                throw new AppException("Ya existe otro registro de ventas para el ejecutivo en el periodo indicado (Año: " + request.getAnio() + ", Mes: " + request.getMes() + ")", 
                        HttpStatus.CONFLICT, "VENTA_PERIODO_DUPLICADO");
            }
        }

        venta.setEjecutivoId(ejecutivoId);
        venta.setAnio(request.getAnio());
        venta.setMes(request.getMes());
        venta.setMeta(request.getMeta());
        if (request.getVentaReal() != null) {
            venta.setVentaReal(request.getVentaReal());
        }
        if (request.getForecast() != null) {
            venta.setForecast(request.getForecast());
        }
        venta.setNotas(request.getNotas());
        venta.setUpdatedById(usuarioActual.getId());
        venta.setUpdatedAt(LocalDateTime.now());

        return mapear(List.of(ventaRepository.save(venta))).get(0);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!securityUtils.esGerenteOAdmin()) {
            throw new AppException("Solo un Gerente o Administrador puede eliminar registros de venta", HttpStatus.FORBIDDEN);
        }
        Venta venta = getVenta(id);
        ventaRepository.delete(venta);
    }

    private Venta getVenta(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new AppException("Registro de ventas no encontrado con id: " + id, HttpStatus.NOT_FOUND));
    }

    private void verificarAcceso(Long ejecutivoId) {
        // Los ejecutivos solo pueden ver o editar sus propias metas y ventas
        UsuarioPrincipal usuarioActual = securityUtils.getUsuarioActual();
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.EJECUTIVO) {
            if (!usuarioActual.getId().equals(ejecutivoId)) {
                throw new AppException("No tienes permiso para ver o modificar los datos de otro ejecutivo", HttpStatus.FORBIDDEN);
            }
        }
    }

    /**
     * Mapea las ventas resolviendo los nombres de usuarios en una sola consulta
     * por lotes en lugar de dos consultas por venta (N+1).
     */
    private List<VentaResponse> mapear(List<Venta> ventas) {
        Set<Long> usuarioIds = new HashSet<>();

        for (Venta v : ventas) {
            usuarioIds.add(v.getEjecutivoId());
            if (v.getUpdatedById() != null) {
                usuarioIds.add(v.getUpdatedById());
            }
        }

        Map<Long, String> usuariosNombres = usuarioRepository.findAllById(usuarioIds).stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u.getNombre() + " " + u.getApellido()));

        return ventas.stream()
                .map(v -> ventaMapper.toResponse(v, usuariosNombres))
                .toList();
    }
}
