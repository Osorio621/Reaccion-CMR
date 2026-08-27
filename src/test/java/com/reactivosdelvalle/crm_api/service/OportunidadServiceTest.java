package com.reactivosdelvalle.crm_api.service;

import com.reactivosdelvalle.crm_api.dto.request.CambioEtapaRequest;
import com.reactivosdelvalle.crm_api.dto.request.CerrarOportunidadRequest;
import com.reactivosdelvalle.crm_api.dto.request.OportunidadRequest;
import com.reactivosdelvalle.crm_api.entity.*;
import com.reactivosdelvalle.crm_api.exception.AppException;
import com.reactivosdelvalle.crm_api.mapper.OportunidadEtapaHistMapper;
import com.reactivosdelvalle.crm_api.mapper.OportunidadMapper;
import com.reactivosdelvalle.crm_api.repository.*;
import com.reactivosdelvalle.crm_api.security.UsuarioPrincipal;
import com.reactivosdelvalle.crm_api.util.SecurityUtils;
import jakarta.persistence.EntityManager;
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
 * Pruebas unitarias de oportunidades: Regla de Oro, asignación de ejecutivo,
 * cambio de etapa y cierre.
 */
@ExtendWith(MockitoExtension.class)
class OportunidadServiceTest {

    @Mock private OportunidadRepository oportunidadRepository;
    @Mock private OportunidadEtapaHistRepository etapaHistRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ProspectoRepository prospectoRepository;
    @Mock private CatalogoRepository catalogoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private EntityManager entityManager;
    @Mock private OportunidadMapper oportunidadMapper;
    @Mock private OportunidadEtapaHistMapper etapaHistMapper;

    private OportunidadService oportunidadService;

    private OportunidadRequest requestValida;

    @BeforeEach
    void setUp() {
        oportunidadService = new OportunidadService(oportunidadRepository, etapaHistRepository,
                clienteRepository, prospectoRepository, catalogoRepository, usuarioRepository,
                securityUtils, entityManager, oportunidadMapper, etapaHistMapper);

        requestValida = new OportunidadRequest();
        requestValida.setNombre("Suministro reactivos anuales");
        requestValida.setClienteId(50L);
        requestValida.setEtapaId(1L);
        requestValida.setValor(new BigDecimal("150000"));
        requestValida.setProbabilidad(30);
        requestValida.setFechaEstimadaCierre(LocalDate.now().plusDays(60));
        requestValida.setProximaAccion("Enviar cotización");
        requestValida.setFechaProximaAccion(LocalDate.now().plusDays(3));
    }

    private UsuarioPrincipal principalConRol(RolUsuario rol, Long id) {
        return UsuarioPrincipal.create(Usuario.builder()
                .id(id).nombre("X").apellido("Y").email("x@x.com")
                .passwordHash("x").rol(rol).build());
    }

    private void contextoCreacion(Long idEjecutivoActual, boolean esGerenteOAdmin) {
        UsuarioPrincipal principal = principalConRol(
                esGerenteOAdmin ? RolUsuario.GERENTE : RolUsuario.EJECUTIVO, idEjecutivoActual);
        when(securityUtils.getUsuarioActual()).thenReturn(principal);
        when(securityUtils.esGerenteOAdmin()).thenReturn(esGerenteOAdmin);

        // Lenient: en el caso "sin nombre" la Regla de Oro falla antes de usarlos
        lenient().when(usuarioRepository.findById(any())).thenReturn(Optional.of(
                Usuario.builder().id(idEjecutivoActual).activo(true).build()));
        lenient().when(catalogoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(catalogo("ETAPA_PIPELINE")));
        lenient().when(clienteRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(new Cliente()));
        lenient().when(oportunidadRepository.save(any())).thenAnswer(inv -> {
            Oportunidad o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });
    }

    private Catalogo catalogo(String tipo) {
        Catalogo c = new Catalogo();
        c.setTipo(tipo);
        c.setActivo(true);
        return c;
    }

    // ===== CREACIÓN =====

    @Test
    @DisplayName("Un ejecutivo crea la oportunidad asignada a sí mismo")
    void crearComoEjecutivoSeAutoasigna() {
        contextoCreacion(7L, false);

        oportunidadService.create(requestValida);

        ArgumentCaptor<Oportunidad> captor = ArgumentCaptor.forClass(Oportunidad.class);
        verify(oportunidadRepository).save(captor.capture());
        assertAll(
                () -> assertEquals(7L, captor.getValue().getEjecutivoId()),
                () -> assertEquals(EstadoOportunidad.ACTIVA, captor.getValue().getEstado(),
                        "Toda oportunidad nueva nace ACTIVA")
        );
    }

    @Test
    @DisplayName("Un gerente puede asignar la oportunidad a otro ejecutivo")
    void crearComoGerenteAsignaAlIndicado() {
        contextoCreacion(2L, true);
        requestValida.setEjecutivoId(9L);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(
                Usuario.builder().id(9L).activo(true).build()));

        oportunidadService.create(requestValida);

        ArgumentCaptor<Oportunidad> captor = ArgumentCaptor.forClass(Oportunidad.class);
        verify(oportunidadRepository).save(captor.capture());
        assertEquals(9L, captor.getValue().getEjecutivoId());
    }

    @Test
    @DisplayName("Regla de Oro: sin nombre no se crea la oportunidad")
    void crearSinNombreRechazado() {
        contextoCreacion(7L, false);
        requestValida.setNombre(null);

        AppException ex = assertThrows(AppException.class,
                () -> oportunidadService.create(requestValida));

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus()),
                () -> assertEquals("REGLA_DE_ORO_INCUMPLIDA", ex.getErrorKey()),
                () -> verify(oportunidadRepository, never()).save(any())
        );
    }

    // ===== CAMBIO DE ETAPA =====

    private Oportunidad oportunidadActiva() {
        return Oportunidad.builder()
                .id(100L)
                .nombre("Op prueba")
                .clienteId(50L)
                .ejecutivoId(7L)
                .etapaId(1L)
                .estado(EstadoOportunidad.ACTIVA)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Cambiar a la misma etapa es rechazado")
    void cambiarALaMismaEtapa() {
        Oportunidad op = oportunidadActiva();
        when(securityUtils.getUsuarioActual()).thenReturn(principalConRol(RolUsuario.EJECUTIVO, 7L));
        when(securityUtils.puedeAccederA(7L)).thenReturn(true);
        when(oportunidadRepository.findByIdAndActivoTrue(100L)).thenReturn(Optional.of(op));

        CambioEtapaRequest request = new CambioEtapaRequest(1L, null);

        AppException ex = assertThrows(AppException.class,
                () -> oportunidadService.cambiarEtapa(100L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(etapaHistRepository, never()).save(any());
    }

    // ===== CIERRE =====

    @Test
    @DisplayName("Cerrar como PERDIDA exige motivo de pérdida")
    void cerrarPerdidaSinMotivo() {
        Oportunidad op = oportunidadActiva();
        when(securityUtils.getUsuarioActual()).thenReturn(principalConRol(RolUsuario.EJECUTIVO, 7L));
        when(securityUtils.puedeAccederA(7L)).thenReturn(true);
        when(oportunidadRepository.findByIdAndActivoTrue(100L)).thenReturn(Optional.of(op));

        CerrarOportunidadRequest request = new CerrarOportunidadRequest(
                EstadoOportunidad.PERDIDA, "  ", LocalDate.now());

        AppException ex = assertThrows(AppException.class,
                () -> oportunidadService.cerrar(100L, request));

        assertTrue(ex.getMessage().contains("motivo"));
        verify(oportunidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cerrar como GANADA fija estado y fecha de cierre real")
    void cerrarGanadaOk() {
        Oportunidad op = oportunidadActiva();
        when(securityUtils.getUsuarioActual()).thenReturn(principalConRol(RolUsuario.EJECUTIVO, 7L));
        when(securityUtils.puedeAccederA(7L)).thenReturn(true);
        when(oportunidadRepository.findByIdAndActivoTrue(100L)).thenReturn(Optional.of(op));
        when(oportunidadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CerrarOportunidadRequest request = new CerrarOportunidadRequest(
                EstadoOportunidad.GANADA, null, null);

        oportunidadService.cerrar(100L, request);

        ArgumentCaptor<Oportunidad> captor = ArgumentCaptor.forClass(Oportunidad.class);
        verify(oportunidadRepository).save(captor.capture());
        assertAll(
                () -> assertEquals(EstadoOportunidad.GANADA, captor.getValue().getEstado()),
                () -> assertEquals(LocalDate.now(), captor.getValue().getFechaCierreReal())
        );
    }
}
