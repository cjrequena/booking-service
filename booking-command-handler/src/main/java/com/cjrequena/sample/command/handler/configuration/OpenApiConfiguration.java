package com.cjrequena.sample.command.handler.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfiguration {

  private final ObjectMapper objectMapper;

  private static final String SECURITY_SCHEME_NAME = "bearerAuth";

  @Bean
  public OpenAPI customOpenAPI() {

    return new OpenAPI()
      .info(new Info()
        .title("Booking Command Handler API")
        .version("v1")
        .description("REST API documentation")
        .contact(new Contact()
          .name("Carlos Requena")
          .email("cjrequena001+git@email.com"))
        .license(new License()
          .name("Apache 2.0")
          .url("https://www.apache.org/licenses/LICENSE-2.0")))
      .externalDocs(new ExternalDocumentation()
        .description("Project Documentation")
        .url("https://example.com/docs"))
      .addSecurityItem(new SecurityRequirement()
        .addList(SECURITY_SCHEME_NAME))
      .components(new Components()
        .addSecuritySchemes(SECURITY_SCHEME_NAME,
          new SecurityScheme()
            .name(SECURITY_SCHEME_NAME)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")));
  }

  @PostConstruct
  public void init() {
    ModelConverters
      .getInstance()
      .addConverter(new ModelResolver(objectMapper));
  }
}
