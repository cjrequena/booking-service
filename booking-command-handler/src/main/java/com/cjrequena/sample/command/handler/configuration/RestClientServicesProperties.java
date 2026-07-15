package com.cjrequena.sample.command.handler.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Outbound REST client configuration, keyed by logical service name.
 *
 * <p>Mirrors the standalone command handler's {@code services.*} properties, but generic:
 * no concrete downstream services are wired yet. Add entries under {@code rest-client.services:}
 * in {@code application.yml} and a corresponding {@code @Bean} in {@link RestClientConfiguration}
 * when a consumer is introduced.</p>
 *
 * <pre>
 * rest-client:
 *   services:
 *     business-service:
 *       base-url: http://localhost:8090
 *       accept-version: application/vnd.business-service.v1
 * </pre>
 *
 * @author cjrequena
 */
@ConfigurationProperties(prefix = "rest-client")
public record RestClientServicesProperties(Map<String, ClientProperties> services) {

  public record ClientProperties(String baseUrl, String acceptVersion) {
  }
}
