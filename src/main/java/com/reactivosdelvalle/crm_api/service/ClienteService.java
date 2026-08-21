package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.ClienteRequest;
import com.reactivosdelvalle.crm_api.dto.request.ContactoRequest;
import com.reactivosdelvalle.crm_api.dto.response.ClienteResponse;
import com.reactivosdelvalle.crm_api.dto.response.ContactoResponse;
import com.reactivosdelvalle.crm_api.entity.Cliente;
import com.reactivosdelvalle.crm_api.entity.Contacto;
import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.ClienteMapper;
import com.reactivosdelvalle.crm_api.mapper.ContactoMapper;
import com.reactivosdelvalle.crm_api.repository.CatalogoRepository;
import com.reactivosdelvalle.crm_api.repository.ClienteRepository;
import com.reactivosdelvalle.crm_api.repository.ContactoRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ContactoRepository contactoRepository;
    private final CatalogoRepository catalogoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;
    private final ClienteMapper clienteMapper;
    private final ContactoMapper contactoMapper;

    @Autowired
    public ClienteService(
            ClienteRepository clienteRepository,
            ContactoRepository contactoRepository,
            CatalogoRepository catalogoRepository,
            UsuarioRepository usuarioRepository,
            SecurityUtils securityUtils,
            ClienteMapper clienteMapper,
            ContactoMapper contactoMapper) {
        this.clienteRepository = clienteRepository;
        this.contactoRepository = contactoRepository;
        this.catalogoRepository = catalogoRepository;
        this.usuarioRepository = usuarioRepository;
        this.securityUtils = securityUtils;
        this.clienteMapper = clienteMapper;
        this.contactoMapper = contactoMapper;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> findAll() {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        List<Cliente> clientes;
        if (usuario != null && usuario.getRol() == RolUsuario.EJECUTIVO) {
            clientes = clienteRepository.findByEjecutivoIdAndActivoTrueOrderByNombreAsc(usuario.getId());
        } else {
            clientes = clienteRepository.findAllByActivoTrueOrderByNombreAsc();
        }
        return clientes.stream().map(clienteMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse findById(Long id) {
        Cliente cliente = getActivo(id);
        verificarAcceso(cliente);
        return clienteMapper.toResponse(cliente);
    }

    @Transactional
    public ClienteResponse create(ClienteRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();

        Long ejecutivoId = usuario.getId();
        if (securityUtils.esGerenteOAdmin() && request.getEjecutivoId() != null) {
            ejecutivoId = request.getEjecutivoId();
        }
        verificarEjecutivoActivo(ejecutivoId);

        validarCatalogos(request.getTipoId(), request.getIndustriaId(), request.getZonaId());

        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .razonSocial(request.getRazonSocial())
                .rfc(request.getRfc())
                .tipoId(request.getTipoId())
                .industriaId(request.getIndustriaId())
                .zonaId(request.getZonaId())
                .ejecutivoId(ejecutivoId)
                .telefonoPrincipal(request.getTelefonoPrincipal())
                .emailPrincipal(request.getEmailPrincipal())
                .sitioWeb(request.getSitioWeb())
                .direccion(request.getDireccion())
                .ciudad(request.getCiudad())
                .estadoRegion(request.getEstadoRegion())
                .notas(request.getNotas())
                .fechaPrimeraCompra(request.getFechaPrimeraCompra())
                .createdById(usuario.getId())
                .updatedById(usuario.getId())
                .build();

        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse update(Long id, ClienteRequest request) {
        UsuarioPrincipal usuario = securityUtils.getUsuarioActual();
        Cliente cliente = getActivo(id);
        verificarAcceso(cliente);

        validarCatalogos(request.getTipoId(), request.getIndustriaId(), request.getZonaId());

        cliente.setNombre(request.getNombre());
        cliente.setRazonSocial(request.getRazonSocial());
        cliente.setRfc(request.getRfc());
        cliente.setTipoId(request.getTipoId());
        cliente.setIndustriaId(request.getIndustriaId());
        cliente.setZonaId(request.getZonaId());
        if (securityUtils.esGerenteOAdmin() && request.getEjecutivoId() != null) {
            verificarEjecutivoActivo(request.getEjecutivoId());
            cliente.setEjecutivoId(request.getEjecutivoId());
        }
        cliente.setTelefonoPrincipal(request.getTelefonoPrincipal());
        cliente.setEmailPrincipal(request.getEmailPrincipal());
        cliente.setSitioWeb(request.getSitioWeb());
        cliente.setDireccion(request.getDireccion());
        cliente.setCiudad(request.getCiudad());
        cliente.setEstadoRegion(request.getEstadoRegion());
        cliente.setNotas(request.getNotas());
        cliente.setFechaPrimeraCompra(request.getFechaPrimeraCompra());
        cliente.setUpdatedById(usuario.getId());

        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void delete(Long id) {
        Cliente cliente = getActivo(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    @Transactional
    public ClienteResponse reasignar(Long id, Long nuevoEjecutivoId) {
        verificarEjecutivoActivo(nuevoEjecutivoId);
        Cliente cliente = getActivo(id);
        cliente.setEjecutivoId(nuevoEjecutivoId);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ContactoResponse> findContactos(Long clienteId) {
        verificarAcceso(getActivo(clienteId));
        return contactoRepository.findByClienteIdAndActivoTrueOrderByNombreAsc(clienteId).stream()
                .map(contactoMapper::toResponse)
                .toList();
    }

    @Transactional
    public ContactoResponse createContacto(Long clienteId, ContactoRequest request) {
        Cliente cliente = getActivo(clienteId);
        verificarAcceso(cliente);

        if (Boolean.TRUE.equals(request.getEsPrincipal())) {
            quitarPrincipal(clienteId);
        }

        Contacto contacto = Contacto.builder()
                .clienteId(clienteId)
                .nombre(request.getNombre())
                .cargo(request.getCargo())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .esPrincipal(Boolean.TRUE.equals(request.getEsPrincipal()))
                .build();

        return contactoMapper.toResponse(contactoRepository.save(contacto));
    }

    @Transactional
    public ContactoResponse updateContacto(Long clienteId, Long contactoId, ContactoRequest request) {
        verificarAcceso(getActivo(clienteId));
        Contacto contacto = contactoRepository.findByIdAndActivoTrue(contactoId)
                .orElseThrow(() -> new AppException("Contacto no encontrado con id: " + contactoId, HttpStatus.NOT_FOUND));
        if (!contacto.getClienteId().equals(clienteId)) {
            throw new AppException("El contacto no pertenece al cliente indicado", HttpStatus.BAD_REQUEST);
        }

        if (Boolean.TRUE.equals(request.getEsPrincipal())) {
            quitarPrincipal(clienteId);
        }

        contacto.setNombre(request.getNombre());
        contacto.setCargo(request.getCargo());
        contacto.setTelefono(request.getTelefono());
        contacto.setEmail(request.getEmail());
        if (request.getEsPrincipal() != null) {
            contacto.setEsPrincipal(request.getEsPrincipal());
        }

        return contactoMapper.toResponse(contactoRepository.save(contacto));
    }

    @Transactional
    public void deleteContacto(Long clienteId, Long contactoId) {
        verificarAcceso(getActivo(clienteId));
        Contacto contacto = contactoRepository.findByIdAndActivoTrue(contactoId)
                .orElseThrow(() -> new AppException("Contacto no encontrado con id: " + contactoId, HttpStatus.NOT_FOUND));
        if (!contacto.getClienteId().equals(clienteId)) {
            throw new AppException("El contacto no pertenece al cliente indicado", HttpStatus.BAD_REQUEST);
        }
        contacto.setActivo(false);
        contactoRepository.save(contacto);
    }

    private void quitarPrincipal(Long clienteId) {
        contactoRepository.quitarPrincipal(clienteId);
    }

    private Cliente getActivo(Long id) {
        return clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new AppException("Cliente no encontrado con id: " + id, HttpStatus.NOT_FOUND));
    }

    private void verificarAcceso(Cliente cliente) {
        if (!securityUtils.puedeAccederA(cliente.getEjecutivoId())) {
            throw new AppException("Este registro pertenece a otro ejecutivo", HttpStatus.FORBIDDEN);
        }
    }

    private void verificarEjecutivoActivo(Long ejecutivoId) {
        usuarioRepository.findById(ejecutivoId)
                .filter(Usuario::getActivo)
                .orElseThrow(() -> new AppException("El ejecutivo indicado no existe o está inactivo", HttpStatus.BAD_REQUEST));
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