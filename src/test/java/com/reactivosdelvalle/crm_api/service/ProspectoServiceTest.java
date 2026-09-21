package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.ConvertirProspectoRequest;
import com.reactivosdelvalle.crm_api.dto.request.OportunidadRequest;
import com.reactivosdelvalle.crm_api.entity.Catalogo;
import com.reactivosdelvalle.crm_api.entity.Prospecto;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.ClienteMapper;
import com.reactivosdelvalle.crm_api.mapper.ProspectoMapper;
import com.reactivosdelvalle.crm_api.repository.CatalogoRepository;
import com.reactivosdelvalle.crm_api.repository.ClienteRepository;
import com.reactivosdelvalle.crm_api.repository.ProspectoRepository;
import com.reactivosdelvalle.crm_api.repository.UsuarioRepository;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de la conversión de prospecto a cliente
 * (con y sin oportunidad inicial opcional).
 */
@ExtendWith(MockitoExtension.class)
class ProspectoServiceTest {

    @Mock private ProspectoRepository prospectoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private CatalogoRepository catalogoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private ProspectoMapper prospectoMapper;
    @Mock private ClienteMapper clienteMapper;
    @Mock private OportunidadService oportunidadService;

    private ProspectoService prospectoService;

    private Prospecto prospecto;

    @BeforeEach
    void setUp() {
        prospectoService = new ProspectoService(prospectoRepository, clienteRepository,
                catalogoRepository, usuarioRepository, securityUtils,
                prospectoMapper, clienteMapper, oportunidadService);

        prospecto = Prospecto.builder()
                .id(10L)
                .nombre("Laboratorio XYZ")
                .responsableId(5L)
                .tipoId(12L)
                .industriaId(18L)
                .zonaId(23L)
                .convertido(false)
                .activo(true)
                .build();

        when(securityUtils.getUsuarioActual()).thenReturn(principalAdmin());
        when(securityUtils.puedeAccederA(5L)).thenReturn(true);
        when(prospectoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(prospecto));

        com.reactivosdelvalle.crm_api.dto.response.ClienteResponse clienteSimulado =
                mock(com.reactivosdelvalle.crm_api.dto.response.ClienteResponse.class);
        lenient().when(clienteSimulado.getId()).thenReturn(99L);
        lenient().when(clienteMapper.toResponse(any())).thenReturn(clienteSimulado);
        lenient().when(clienteRepository.save(any())).thenAnswer(inv -> {
            com.reactivosdelvalle.crm_api.entity.Cliente c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });
    }

    private UsuarioPrincipal principalAdmin() {
        return UsuarioPrincipal.create(
                com.reactivosdelvalle.crm_api.entity.Usuario.builder()
                        .id(1L).nombre("Admin").apellido("Sistema")
                        .email("admin@x.com").passwordHash("x")
                        .rol(com.reactivosdelvalle.crm_api.entity.RolUsuario.ADMIN)
                        .build());
    }

    private void catalogosValidos() {
        when(catalogoRepository.findByIdAndActivoTrue(12L)).thenReturn(Optional.of(catalogo("TIPO_CLIENTE")));
        when(catalogoRepository.findByIdAndActivoTrue(18L)).thenReturn(Optional.of(catalogo("INDUSTRIA")));
        when(catalogoRepository.findByIdAndActivoTrue(23L)).thenReturn(Optional.of(catalogo("ZONA_GEOGRAFICA")));
    }

    private Catalogo catalogo(String tipo) {
        Catalogo c = new Catalogo();
        c.setTipo(tipo);
        c.setActivo(true);
        return c;
    }

    // ===== CONVERSIÓN SIN OPORTUNIDAD =====

    @Test
    @DisplayName("Convertir sin bloque de oportunidad crea solo el cliente")
    void convertirSinOportunidad() {
        catalogosValidos();

        ConvertirProspectoRequest request = new ConvertirProspectoRequest();
        request.setRazonSocial("Laboratorio XYZ SA");

        var respuesta = prospectoService.convertir(10L, request);

        assertAll(
                () -> assertEquals(99L, respuesta.getCliente().getId()),
                () -> assertNull(respuesta.getOportunidad(), "No debe crearse oportunidad si no se solicitó"),
                () -> verify(oportunidadService, never()).create(any()),
                // El prospecto queda marcado como convertido y vinculado al cliente
                () -> assertTrue(prospecto.getConvertido()),
                () -> assertEquals(99L, prospecto.getClienteId())
        );
    }

    // ===== CONVERSIÓN CON OPORTUNIDAD =====

    @Test
    @DisplayName("Convertir con bloque crea también la oportunidad vinculada")
    void convertirConOportunidad() {
        catalogosValidos();
        when(oportunidadService.create(any(OportunidadRequest.class))).thenReturn(null);

        ConvertirProspectoRequest.OportunidadInicial bloque =
                new ConvertirProspectoRequest.OportunidadInicial();
        bloque.setNombre("Suministro reactivos anuales");
        bloque.setEtapaId(1L);
        bloque.setValor(new BigDecimal("150000"));
        bloque.setProbabilidad(30);
        bloque.setFechaEstimadaCierre(LocalDate.now().plusDays(60));
        bloque.setProximaAccion("Enviar cotización");
        bloque.setFechaProximaAccion(LocalDate.now().plusDays(3));

        ConvertirProspectoRequest request = new ConvertirProspectoRequest();
        request.setOportunidad(bloque);

        prospectoService.convertir(10L, request);

        ArgumentCaptor<OportunidadRequest> captor = ArgumentCaptor.forClass(OportunidadRequest.class);
        verify(oportunidadService).create(captor.capture());
        OportunidadRequest enviada = captor.getValue();

        assertAll(
                () -> assertEquals("Suministro reactivos anuales", enviada.getNombre()),
                () -> assertEquals(99L, enviada.getClienteId(), "Debe apuntar al cliente recién creado"),
                () -> assertEquals(10L, enviada.getProspectoId(), "Debe quedar vinculada al prospecto"),
                () -> assertEquals(5L, enviada.getEjecutivoId(),
                        "El ejecutivo debe ser el responsable del prospecto, no quien convierte"),
                () -> assertEquals(new BigDecimal("150000"), enviada.getValor())
        );
    }

    // ===== REGLAS DE NEGOCIO =====

    @Test
    @DisplayName("No se puede convertir un prospecto ya convertido")
    void prospectoYaConvertido() {
        prospecto.setConvertido(true);
        ConvertirProspectoRequest request = new ConvertirProspectoRequest();

        AppException ex = assertThrows(AppException.class,
                () -> prospectoService.convertir(10L, request));

        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, ex.getStatus()),
                () -> assertEquals("PROSPECTO_CONVERTIDO", ex.getErrorKey())
        );
    }

    @Test
    @DisplayName("Un ejecutivo no puede convertir prospectos ajenos")
    void accesoDenegadoAOtroEjecutivo() {
        when(securityUtils.puedeAccederA(5L)).thenReturn(false);
        ConvertirProspectoRequest request = new ConvertirProspectoRequest();

        AppException ex = assertThrows(AppException.class,
                () -> prospectoService.convertir(10L, request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("El tipo de cliente indicado debe existir y estar activo")
    void tipoClienteInvalidoRechazado() {
        when(catalogoRepository.findByIdAndActivoTrue(12L)).thenReturn(Optional.empty());

        ConvertirProspectoRequest request = new ConvertirProspectoRequest();
        request.setTipoId(12L);

        AppException ex = assertThrows(AppException.class,
                () -> prospectoService.convertir(10L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(clienteRepository, never()).save(any());
    }
}
