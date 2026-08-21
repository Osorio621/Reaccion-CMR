package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.CatalogoRequest;
import com.reactivosdelvalle.crm_api.dto.response.CatalogoResponse;
import com.reactivosdelvalle.crm_api.entity.Catalogo;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.CatalogoMapper;
import com.reactivosdelvalle.crm_api.repository.CatalogoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogoService {

    private final CatalogoRepository catalogoRepository;
    private final CatalogoMapper catalogoMapper;

    @Autowired
    public CatalogoService(CatalogoRepository catalogoRepository, CatalogoMapper catalogoMapper) {
        this.catalogoRepository = catalogoRepository;
        this.catalogoMapper = catalogoMapper;
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponse> findAll() {
        return catalogoRepository.findAllByOrderByTipoAscOrdenAsc().stream()
                .map(catalogoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponse> findByTipo(String tipo) {
        List<Catalogo> catalogos = catalogoRepository.findByTipoAndActivoTrueOrderByOrdenAsc(tipo);
        if (catalogos.isEmpty()) {
            throw new AppException("No se encontraron catálogos para el tipo: " + tipo, HttpStatus.NOT_FOUND);
        }
        return catalogos.stream()
                .map(catalogoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogoResponse findById(Long id) {
        return catalogoMapper.toResponse(getActivo(id));
    }

    @Transactional
    public CatalogoResponse create(CatalogoRequest request) {
        if (catalogoRepository.existsByCodigo(request.getCodigo())) {
            throw new AppException("Ya existe un catálogo con el código: " + request.getCodigo(), HttpStatus.CONFLICT, "CODIGO_DUPLICADO");
        }

        Catalogo catalogo = Catalogo.builder()
                .tipo(request.getTipo())
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .probabilidadDefault(request.getProbabilidadDefault())
                .orden(request.getOrden() != null ? request.getOrden() : 0)
                .build();

        return catalogoMapper.toResponse(catalogoRepository.save(catalogo));
    }

    @Transactional
    public CatalogoResponse update(Long id, CatalogoRequest request) {
        Catalogo catalogo = getActivo(id);

        if (catalogoRepository.existsByCodigoAndIdNot(request.getCodigo(), id)) {
            throw new AppException("Ya existe un catálogo con el código: " + request.getCodigo(), HttpStatus.CONFLICT, "CODIGO_DUPLICADO");
        }

        catalogo.setTipo(request.getTipo());
        catalogo.setCodigo(request.getCodigo());
        catalogo.setNombre(request.getNombre());
        catalogo.setDescripcion(request.getDescripcion());
        catalogo.setProbabilidadDefault(request.getProbabilidadDefault());
        catalogo.setOrden(request.getOrden() != null ? request.getOrden() : 0);

        return catalogoMapper.toResponse(catalogoRepository.save(catalogo));
    }

    @Transactional
    public void delete(Long id) {
        Catalogo catalogo = getActivo(id);
        catalogo.setActivo(false);
        catalogoRepository.save(catalogo);
    }

    private Catalogo getActivo(Long id) {
        return catalogoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new AppException("Catálogo no encontrado con id: " + id, HttpStatus.NOT_FOUND));
    }
}