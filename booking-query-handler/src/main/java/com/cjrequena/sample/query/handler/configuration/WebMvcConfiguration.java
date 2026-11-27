package com.cjrequena.sample.query.handler.configuration;

import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
  private final JsonMapper mapper;

  @Override
  public void configureMessageConverters(HttpMessageConverters.ServerBuilder converters) {
    converters.withJsonConverter(new JacksonJsonHttpMessageConverter(mapper));
  }

  @Override
  public void addCorsMappings(CorsRegistry cors) {
    cors.addMapping("/**")
      .allowedOrigins("*")
      .allowedMethods("*")
      .maxAge(3600)
      .exposedHeaders("total-count")
      .allowedHeaders("*");
  }
}
