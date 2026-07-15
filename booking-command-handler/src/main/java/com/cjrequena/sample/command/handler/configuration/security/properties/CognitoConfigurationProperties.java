package com.cjrequena.sample.command.handler.configuration.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cognito JWT resource-server configuration.
 *
 * <p>Adapted from the standalone command handler. When {@code enabled} is {@code true} and an
 * {@code issuer-uri} is provided, the non-local security chain validates bearer JWTs against the
 * issuer's JWKS. Disabled by default so existing (unauthenticated) behaviour is preserved.</p>
 *
 * @param enabled   whether Cognito JWT validation is active on non-local profiles
 * @param issuerUri the token issuer location (e.g. https://cognito-idp.&lt;region&gt;.amazonaws.com/&lt;pool-id&gt;)
 * @author cjrequena
 */
@ConfigurationProperties(prefix = "security.cognito")
public record CognitoConfigurationProperties(boolean enabled, String issuerUri) {
}
