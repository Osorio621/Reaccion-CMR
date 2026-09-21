package com.reactivosdelvalle.crm_api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para todos los tests de integración.
 *
 * Patrón singleton: el contenedor se inicia UNA sola vez en el bloque static
 * y vive hasta que el JVM termina (Ryuk lo limpia). Así Spring puede cachear
 * el ApplicationContext entre clases de test sin que el puerto cambie.
 *
 * Sin @Testcontainers ni @Container para evitar que JUnit detenga el
 * contenedor al finalizar cada clase y rompa el cache de contexto.
 *
 * @ServiceConnection configura el DataSource via ConnectionDetails.
 * @DynamicPropertySource también registra spring.datasource.* como propiedades
 * reales para que los @Value en BackupService (y similares) se resuelvan.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("crm_test")
                    .withUsername("crm_user")
                    .withPassword("crm_test_pass");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
