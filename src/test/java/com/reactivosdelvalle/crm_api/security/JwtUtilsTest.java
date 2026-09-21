package com.reactivosdelvalle.crm_api.security;

import com.reactivosdelvalle.crm_api.entity.RolUsuario;
import com.reactivosdelvalle.crm_api.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de generación y validación de tokens JWT.
 */
class JwtUtilsTest {

    private static final String SECRETO_PRUEBA =
            "clave-de-prueba-suficientemente-larga-para-hmac-sha256-1234567890";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        // Access token 1h, refresh token 7 días (como en producción)
        jwtUtils = new JwtUtils(SECRETO_PRUEBA, 3_600_000L, 604_800_000L);
    }

    private UsuarioPrincipal principalEjemplo() {
        Usuario usuario = Usuario.builder()
                .id(42L)
                .nombre("Ana")
                .apellido("Gómez")
                .email("ana@reactivosdelvalle.com")
                .passwordHash("$2a$hash")
                .rol(RolUsuario.EJECUTIVO)
                .build();
        return UsuarioPrincipal.create(usuario);
    }

    @Test
    @DisplayName("Access token válido se genera y recupera el id del usuario")
    void accessTokenRoundtrip() {
        String token = jwtUtils.generateAccessToken(principalEjemplo());

        assertAll(
                () -> assertTrue(jwtUtils.validateToken(token)),
                () -> assertEquals("42", jwtUtils.getUserIdFromToken(token))
        );
    }

    @Test
    @DisplayName("Refresh token válido se genera y recupera el id del usuario")
    void refreshTokenRoundtrip() {
        String token = jwtUtils.generateRefreshToken(principalEjemplo());

        assertAll(
                () -> assertTrue(jwtUtils.validateToken(token)),
                () -> assertEquals("42", jwtUtils.getUserIdFromToken(token))
        );
    }

    @Test
    @DisplayName("Un token malformado no valida")
    void tokenMalformadoNoValida() {
        assertFalse(jwtUtils.validateToken("esto.no.es.un.jwt"));
    }

    @Test
    @DisplayName("Un token nulo o vacío no valida")
    void tokenNuloNoValida() {
        assertFalse(jwtUtils.validateToken(null));
        assertFalse(jwtUtils.validateToken(""));
    }

    @Test
    @DisplayName("Un token expirado no valida")
    void tokenExpiradoNoValida() {
        // Emisor configurado con expiración negativa => todo token nace expirado
        JwtUtils emisorExpirado = new JwtUtils(SECRETO_PRUEBA, -10_000L, -10_000L);
        String token = emisorExpirado.generateRefreshToken(principalEjemplo());

        assertFalse(jwtUtils.validateToken(token));
    }

    @Test
    @DisplayName("Tokens firmados con otra clave no validan")
    void firmaDistintaNoValida() {
        JwtUtils otroEmisor = new JwtUtils(
                "otra-clave-de-prueba-completamente-distinta-y-larga-987654321",
                3_600_000L, 604_800_000L);
        String tokenAjeno = otroEmisor.generateAccessToken(principalEjemplo());

        assertFalse(jwtUtils.validateToken(tokenAjeno));
    }
}
