package com.cjrequena.sample.command.handler.configuration.security;

import com.cjrequena.sample.command.handler.configuration.security.properties.CognitoConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Servlet HTTP security for the command handler (runs on {@code spring-boot-starter-web}).
 *
 * <p>Adapted from the standalone command handler's security stack. Two mutually-exclusive,
 * profile-gated {@link SecurityFilterChain} beans keep exactly one {@code anyRequest} chain active:</p>
 * <ul>
 *   <li>{@code local} / {@code integrationTest} — permits everything, preserving the service's
 *       pre-security (unauthenticated) behaviour.</li>
 *   <li>every other profile — authenticates all requests except a public allowlist. When
 *       {@code security.cognito.enabled=true} with an issuer, bearer JWTs are validated via JWKS.</li>
 * </ul>
 *
 * @author cjrequena
 */
@Slf4j
@Configuration
@EnableWebSecurity
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
  public SecurityFilterChain permissiveSecurityFilterChain(HttpSecurity http) throws Exception {
    log.info("Security: permissive filter chain (local/integrationTest) — all requests permitted");
    http
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(registry -> registry.anyRequest().permitAll());
    return http.build();
  }

  @Profile("!local & !integrationTest")
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, CognitoConfigurationProperties cognito) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(registry -> registry
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers(PUBLIC_PATHS).permitAll()
        .anyRequest().authenticated());

    if (cognito.enabled() && cognito.issuerUri() != null && !cognito.issuerUri().isBlank()) {
      log.info("Security: Cognito JWT resource server enabled, issuer={}", cognito.issuerUri());
      http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder(cognito))));
    } else {
      log.warn("Security: Cognito disabled — non-public requests will be rejected. "
        + "Set security.cognito.enabled=true and security.cognito.issuer-uri to enable JWT authentication.");
    }
    return http.build();
  }

  private JwtDecoder jwtDecoder(CognitoConfigurationProperties cognito) {
    return JwtDecoders.fromIssuerLocation(cognito.issuerUri());
  }
}
