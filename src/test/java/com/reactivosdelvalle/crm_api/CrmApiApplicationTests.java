package com.reactivosdelvalle.crm_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de humo: verifica que el contexto levanta con la BD real (Testcontainers),
 * las 17 migraciones Flyway corren sin errores, y las rutas básicas responden.
 */
class CrmApiApplicationTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Contexto levanta: PostgreSQL en Docker + Flyway (17 migraciones) + Spring OK")
    void contextLoads() {
        // Si este test pasa: contexto completo con BD real, sin errores de migración
    }

    @Test
    @DisplayName("Login con cuerpo vacío retorna 400")
    void loginCuerpoVacioRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Endpoints protegidos retornan 401 sin token JWT")
    void endpointProtegidoSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Pipeline retorna 401 sin token JWT")
    void pipelineSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/pipeline"))
                .andExpect(status().isUnauthorized());
    }
}
