package com.votoeletronico.voto.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Votação Eletrônica - API")
                        .version("1.0.0")
                        .description("""
                                API RESTful para sistema de votação eletrônica seguro e auditável.

                                ## Características
                                - Votação anônima com preservação de privacidade
                                - Sistema de auditoria com hash encadeado
                                - Autenticação segura com OAuth2
                                - Criptografia de votos

                                ## Segurança
                                Todas as rotas de administração requerem autenticação OAuth2.
                                """)
                        .contact(new Contact()
                                .name("Equipe Voto Eletrônico")
                                .email("contato@voto.local"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://voto.local/license")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Desenvolvimento"),
                        new Server().url("https://api-staging.voto.local").description("Staging"),
                        new Server().url("https://api.voto.local").description("Produção")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")));
    }
}
