package com.cjrequena.sample.query.handler.configuration.security;

import com.cjrequena.sample.query.handler.configuration.security.properties.CognitoConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive HTTP security for the query handler (runs on {@code spring-boot-starter-webflux}).
 *
 * <p>Reactive counterpart of the standalone query handler's security stack. Two mutually-exclusive,
 * profile-gated {@link SecurityWebFilterChain} beans keep exactly one {@code anyExchange} chain active:</p>
 * <ul>
 *   <li>{@code local} / {@code integrationTest} — permits everything, preserving the service's
 *       pre-security (unauthenticated) behaviour.</li>
 *   <li>every other profile — authenticates all exchanges except a public allowlist. When
 *       {@code security.cognito.enabled=true} with an issuer, bearer JWTs are validated via JWKS.</li>
 * </ul>
 *
 * @author cjrequena
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(CognitoConfigurationProperties.class)
public class SecurityConfiguration {

  /** Paths reachable without authentication (health, API docs). */
  private static final String[] PUBLIC_PATHS = {
    "/management/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**",
    "/webjars/**"
  };

  @Profile({"local", "integrationTest"})
  @Bean
  public SecurityWebFilterChain permissiveSecurityWebFilterChain(ServerHttpSecurity http) {
    log.info("Security: permissive filter chain (local/integrationTest) — all exchanges permitted");
    return http
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
      .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
      .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
      .build();
  }

  @Profile("!local & !integrationTest")
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, CognitoConfigurationProperties cognito) {
    http
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
      .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
      .authorizeExchange(exchanges -> exchanges
        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .pathMatchers(PUBLIC_PATHS).permitAll()
        .anyExchange().authenticated());

    if (cognito.enabled() && cognito.issuerUri() != null && !cognito.issuerUri().isBlank()) {
      log.info("Security: Cognito JWT resource server enabled, issuer={}", cognito.issuerUri());
      http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder(cognito))));
    } else {
      log.warn("Security: Cognito disabled — non-public exchanges will be rejected. "
        + "Set security.cognito.enabled=true and security.cognito.issuer-uri to enable JWT authentication.");
    }
    return http.build();
  }

  private ReactiveJwtDecoder reactiveJwtDecoder(CognitoConfigurationProperties cognito) {
    return ReactiveJwtDecoders.fromIssuerLocation(cognito.issuerUri());
  }
}
