package com.ucak.tprest.config;

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

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API REST Sécurisée - UCAK")
                        .description("API REST développée avec Spring Boot, PostgreSQL et JWT pour la gestion des produits et des utilisateurs (Module L3 JAVA EE - SAO).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Département Informatique UCAK")
                                .email("contact@ucak.edu.sn")
                        )
                )
                // Définition du schéma de sécurité Bearer JWT
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Entrez le jeton JWT sans le préfixe 'Bearer '")
                        )
                );
    }
}
