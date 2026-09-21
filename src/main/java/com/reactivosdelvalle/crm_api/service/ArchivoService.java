package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.response.ArchivoResponse;
import com.reactivosdelvalle.crm_api.entity.Archivo;
import com.reactivosdelvalle.crm_api.entity.TipoEntidadArchivo;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.repository.*;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.storage.ArchivoStorage;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArchivoService {

    private final ArchivoRepository archivoRepository;
    private final ArchivoStorage storage;
    private final SecurityUtils securityUtils;
    private final ClienteRepository clienteRepository;
    private final ProspectoRepository prospectoRepository;
    private final OportunidadRepository oportunidadRepository;
    private final VisitaRepository visitaRepository;

    @Value("${app.storage.max-tamano-mb:30}")
    private long maxTamanoMb;

    @Value("${app.storage.tipos-permitidos:pdf,doc,docx,xls,xlsx,jpg,jpeg,png,gif,zip}")
    private String tiposPermitidos;

    public ArchivoService(ArchivoRepository archivoRepository,
                          ArchivoStorage storage,
                          SecurityUtils securityUtils,
                          ClienteRepository clienteRepository,
                          ProspectoRepository prospectoRepository,
                          OportunidadRepository oportunidadRepository,
                          VisitaRepository visitaRepository) {
        this.archivoRepository = archivoRepository;
        this.storage = storage;
        this.securityUtils = securityUtils;
        this.clienteRepository = clienteRepository;
        this.prospectoRepository = prospectoRepository;
        this.oportunidadRepository = oportunidadRepository;
        this.visitaRepository = visitaRepository;
    }

    @Transactional
    public ArchivoResponse subir(TipoEntidadArchivo tipo, Long entidadId,
                                  MultipartFile archivo, String descripcion) {
        verificarAcceso(tipo, entidadId);
        validarArchivo(archivo);

        String rutaAlmacenada;
        try {
            rutaAlmacenada = storage.guardar(archivo, tipo.name(), entidadId);
        } catch (IOException ex) {
            throw new AppException("No se pudo guardar el archivo: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        Archivo entidad = Archivo.builder()
                .entidadTipo(tipo)
                .entidadId(entidadId)
                .nombreOriginal(archivo.getOriginalFilename())
                .rutaAlmacenada(rutaAlmacenada)
                .tipoMime(archivo.getContentType())
                .tamanoBytes(archivo.getSize())
                .descripcion(descripcion)
                .subidoPorId(usuario != null ? usuario.getId() : null)
                .build();

        return mapear(archivoRepository.save(entidad));
    }

    @Transactional(readOnly = true)
    public List<ArchivoResponse> listar(TipoEntidadArchivo tipo, Long entidadId) {
        verificarAcceso(tipo, entidadId);
        return archivoRepository
                .findByEntidadTipoAndEntidadIdOrderByCreatedAtDesc(tipo, entidadId)
                .stream().map(this::mapear).collect(Collectors.toList());
    }

    @Transactional
    public void eliminar(Long archivoId) {
        Archivo archivo = archivoRepository.findById(archivoId)
                .orElseThrow(() -> new AppException("Archivo no encontrado", HttpStatus.NOT_FOUND));

        verificarAcceso(archivo.getEntidadTipo(), archivo.getEntidadId());

        try {
            storage.eliminar(archivo.getRutaAlmacenada());
        } catch (IOException ex) {
            throw new AppException("No se pudo eliminar el archivo: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        archivoRepository.delete(archivo);
    }

    @Transactional(readOnly = true)
    public DescargaArchivo descargar(Long archivoId) {
        Archivo archivo = archivoRepository.findById(archivoId)
                .orElseThrow(() -> new AppException("Archivo no encontrado", HttpStatus.NOT_FOUND));

        verificarAcceso(archivo.getEntidadTipo(), archivo.getEntidadId());

        Resource recurso;
        try {
            recurso = storage.cargar(archivo.getRutaAlmacenada());
        } catch (IOException ex) {
            throw new AppException("No se pudo cargar el archivo: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new DescargaArchivo(recurso, archivo.getNombreOriginal(), archivo.getTipoMime());
    }

    // --- Helpers ---

    private void verificarAcceso(TipoEntidadArchivo tipo, Long entidadId) {
        if (securityUtils.esGerenteOAdmin()) return;

        Long ejecutivoIdEntidad = obtenerEjecutivoIdEntidad(tipo, entidadId);
        if (!securityUtils.esPropietario(ejecutivoIdEntidad)) {
            throw new AppException("Sin permiso para acceder a esta entidad", HttpStatus.FORBIDDEN);
        }
    }

    private Long obtenerEjecutivoIdEntidad(TipoEntidadArchivo tipo, Long entidadId) {
        return switch (tipo) {
            case CLIENTE -> clienteRepository.findById(entidadId)
                    .orElseThrow(() -> new AppException("Cliente no encontrado", HttpStatus.NOT_FOUND))
                    .getEjecutivoId();
            case PROSPECTO -> prospectoRepository.findById(entidadId)
                    .orElseThrow(() -> new AppException("Prospecto no encontrado", HttpStatus.NOT_FOUND))
                    .getResponsableId();
            case OPORTUNIDAD -> oportunidadRepository.findById(entidadId)
                    .orElseThrow(() -> new AppException("Oportunidad no encontrada", HttpStatus.NOT_FOUND))
                    .getEjecutivoId();
            case VISITA -> visitaRepository.findById(entidadId)
                    .orElseThrow(() -> new AppException("Visita no encontrada", HttpStatus.NOT_FOUND))
                    .getEjecutivoId();
        };
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new AppException("El archivo está vacío", HttpStatus.BAD_REQUEST);
        }

        long maxBytes = maxTamanoMb * 1024 * 1024;
        if (archivo.getSize() > maxBytes) {
            throw new AppException(
                    "El archivo supera el límite de " + maxTamanoMb + " MB", HttpStatus.BAD_REQUEST);
        }

        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.contains(".")) {
            throw new AppException("El archivo no tiene extensión", HttpStatus.BAD_REQUEST);
        }

        String extension = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase();
        Set<String> permitidos = Arrays.stream(tiposPermitidos.split(","))
                .map(String::trim).collect(Collectors.toSet());

        if (!permitidos.contains(extension)) {
            throw new AppException(
                    "Tipo de archivo no permitido: ." + extension +
                    ". Permitidos: " + tiposPermitidos, HttpStatus.BAD_REQUEST);
        }
    }

    private ArchivoResponse mapear(Archivo a) {
        return new ArchivoResponse(
                a.getId(), a.getEntidadTipo().name(), a.getEntidadId(),
                a.getNombreOriginal(), a.getTipoMime(), a.getTamanoBytes(),
                a.getDescripcion(), a.getSubidoPorId(), a.getCreatedAt());
    }

    public record DescargaArchivo(Resource recurso, String nombreOriginal, String tipoMime) {}
}
