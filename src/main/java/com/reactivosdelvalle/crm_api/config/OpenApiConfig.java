package com.reactivosdelvalle.crm_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CRM Reactivos del Valle - API")
                        .description("""
                                API REST del CRM. Autenticación JWT: haga login en \
                                /api/auth/login, copie el accessToken y péguelo con el \
                                botón Authorize de esta página como "Bearer <token>".
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Reactivos del Valle S.A.S")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT,
                                new SecurityScheme()
                                        .name(ESQUEMA_JWT)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token de acceso emitido por /api/auth/login")));
    }
}
