package com.cjrequena.sample.command.handler.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Factory for outbound REST clients to external services, adapted from the standalone
 * command handler's {@code RestClientConfiguration}.
 *
 * <p>The command handler runs on the servlet stack ({@code spring-boot-starter-web}), so the
 * synchronous {@link RestClient} is used (the reactive query handler would use {@code WebClient}).</p>
 *
 * <p><b>Scaffold only:</b> define a named {@code @Bean} per downstream service once a consumer
 * exists, e.g.</p>
 * <pre>
 * &#64;Bean("businessServiceRestClient")
 * public RestClient businessServiceRestClient(RestClientServicesProperties properties) {
 *   return restClient(properties.services().get("business-service"));
 * }
 * </pre>
 *
 * @author cjrequena
 */
@Configuration
@EnableConfigurationProperties(RestClientServicesProperties.class)
public class RestClientConfiguration {

  /**
   * Builds a {@link RestClient} for a downstream service using its base URL and, when present,
   * a default {@code accept-version} header.
   */
  protected RestClient restClient(RestClientServicesProperties.ClientProperties properties) {
    RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
    if (properties.acceptVersion() != null) {
      builder.defaultHeader("accept-version", properties.acceptVersion());
    }
    return builder.build();
  }
}
