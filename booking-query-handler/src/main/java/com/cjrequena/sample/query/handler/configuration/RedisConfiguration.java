package com.cjrequena.sample.query.handler.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redis configuration for standalone, sentinel, and cluster modes.
 * Provides properly configured RedisTemplate, StringRedisTemplate, and RedisCommands beans.
 *
 * <p>Supports three deployment modes:
 * <ul>
 *   <li>Standalone - Single Redis instance</li>
 *   <li>Sentinel - High availability with automatic failover</li>
 *   <li>Cluster - Horizontal scaling with data sharding</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Connection pooling with configurable parameters</li>
 *   <li>Proper JSON serialization with Java 8 date/time support</li>
 *   <li>SSL support</li>
 *   <li>Automatic reconnection</li>
 *   <li>Health checks and monitoring</li>
 * </ul>
 *
 * @author cjrequena
 */
@Slf4j
@Configuration
@EnableRedisRepositories
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisConfiguration {

  private final RedisConfigurationProperties properties;

  private ClientResources clientResources;
  private StatefulRedisConnection<String, String> redisCommandsConnection;

  /**
   * Creates client resources for Lettuce client.
   * These resources are shared across all connections.
   */
  @Bean(destroyMethod = "shutdown")
  public ClientResources clientResources() {
    this.clientResources = DefaultClientResources.builder()
      .ioThreadPoolSize(4)
      .computationThreadPoolSize(4)
      .build();

    log.info("Created ClientResources with 4 IO threads and 4 computation threads");
    return this.clientResources;
  }

  /**
   * Creates a LettuceConnectionFactory based on the configuration.
   * Supports standalone, sentinel, and cluster modes.
   *
   * @return configured LettuceConnectionFactory
   * @throws IllegalStateException if Redis configuration is invalid
   */
  @Bean("redisConnectionFactory")
  @Primary
  public LettuceConnectionFactory redisConnectionFactory(ClientResources clientResources) {
    log.info("Initializing Redis connection factory");

    LettuceConnectionFactory factory;

    // Cluster mode configuration
    if (isClusterMode()) {
      log.info("Configuring Redis Cluster mode");
      factory = createClusterConnectionFactory(clientResources);
    }
    // Sentinel mode configuration
    else if (isSentinelMode()) {
      log.info("Configuring Redis Sentinel mode");
      factory = createSentinelConnectionFactory(clientResources);
    }
    // Standalone mode configuration (default)
    else {
      log.info("Configuring Redis Standalone mode");
      factory = createStandaloneConnectionFactory(clientResources);
    }

    // Share native connections across multiple RedisTemplate instances
    factory.setShareNativeConnection(true);

    // Validate connections before use
    factory.setValidateConnection(true);

    // Initialize the factory
    factory.afterPropertiesSet();

    log.info("Redis connection factory initialized successfully");
    return factory;
  }

  /**
   * Checks if cluster mode is configured.
   */
  private boolean isClusterMode() {
    return properties.getCluster() != null
      && properties.getCluster().getNodes() != null
      && !properties.getCluster().getNodes().isEmpty();
  }

  /**
   * Checks if sentinel mode is configured.
   */
  private boolean isSentinelMode() {
    return properties.getSentinel() != null
      && properties.getSentinel().getNodes() != null
      && !properties.getSentinel().getNodes().isEmpty();
  }

  /**
   * Creates a connection factory for Redis Cluster mode.
   */
  private LettuceConnectionFactory createClusterConnectionFactory(ClientResources clientResources) {
    RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(
      properties.getCluster().getNodes()
    );

    // Set max redirects for cluster operations
    Integer maxRedirects = properties.getCluster().getMaxRedirects();
    if (maxRedirects != null && maxRedirects > 0) {
      clusterConfig.setMaxRedirects(maxRedirects);
    }

    // Set password if configured
    if (StringUtils.hasText(properties.getPassword())) {
      clusterConfig.setPassword(RedisPassword.of(properties.getPassword()));
    }

    log.debug("Cluster nodes: {}, max redirects: {}",
      properties.getCluster().getNodes(), maxRedirects);

    return new LettuceConnectionFactory(
      clusterConfig,
      lettuceClientConfiguration(clientResources)
    );
  }

  /**
   * Creates a connection factory for Redis Sentinel mode.
   */
  private LettuceConnectionFactory createSentinelConnectionFactory(ClientResources clientResources) {
    String masterName = properties.getSentinel().getMaster();

    if (!StringUtils.hasText(masterName)) {
      throw new IllegalStateException("Sentinel master name must be configured");
    }

    RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
      .master(masterName);

    // Add sentinel nodes
    properties.getSentinel().getNodes().forEach(node -> {
      String[] parts = node.split(":");
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid sentinel node format: " + node +
          " (expected format: host:port)");
      }

      try {
        String host = parts[0].trim();
        int port = Integer.parseInt(parts[1].trim());
        sentinelConfig.sentinel(host, port);
        log.debug("Added sentinel node: {}:{}", host, port);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid port number in sentinel node: " + node, e);
      }
    });

    // Set password if configured
    if (StringUtils.hasText(properties.getPassword())) {
      sentinelConfig.setPassword(RedisPassword.of(properties.getPassword()));
    }

    sentinelConfig.setDatabase(properties.getDatabase());

    log.debug("Sentinel master: {}, nodes: {}, database: {}",
      masterName, properties.getSentinel().getNodes(), properties.getDatabase());

    return new LettuceConnectionFactory(
      sentinelConfig,
      lettuceClientConfiguration(clientResources)
    );
  }

  /**
   * Creates a connection factory for Redis Standalone mode.
   */
  private LettuceConnectionFactory createStandaloneConnectionFactory(ClientResources clientResources) {
    String host = properties.getHost();
    Integer port = properties.getPort();

    if (!StringUtils.hasText(host)) {
      throw new IllegalStateException("Redis host must be configured");
    }

    if (port == null || port <= 0) {
      throw new IllegalStateException("Redis port must be configured and positive");
    }

    RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);

    // Set password if configured
    if (StringUtils.hasText(properties.getPassword())) {
      standaloneConfig.setPassword(RedisPassword.of(properties.getPassword()));
    }

    standaloneConfig.setDatabase(properties.getDatabase());

    log.debug("Redis host: {}, port: {}, database: {}", host, port, properties.getDatabase());

    return new LettuceConnectionFactory(
      standaloneConfig,
      lettuceClientConfiguration(clientResources)
    );
  }

  /**
   * Configures the Lettuce client with timeout, SSL, and pooling settings.
   */
  private LettuceClientConfiguration lettuceClientConfiguration(ClientResources clientResources) {

    // Configure socket options
    SocketOptions socketOptions = SocketOptions.builder()
      .connectTimeout(Duration.ofMillis(properties.getTimeout() != null ? properties.getTimeout() : 10000))
      .keepAlive(true)
      .build();

    // Configure client options
    ClientOptions clientOptions = ClientOptions.builder()
      .socketOptions(socketOptions)
      .autoReconnect(true)
      .build();

    // Check if pooling is configured
    boolean poolingEnabled = properties.getLettuce() != null
      && properties.getLettuce().getPool() != null;

    if (poolingEnabled) {
      log.info("Connection pooling enabled");
      return createPoolingClientConfiguration(clientResources, clientOptions);
    } else {
      log.info("Connection pooling disabled");
      return createSimpleClientConfiguration(clientResources, clientOptions);
    }
  }

  /**
   * Creates client configuration with connection pooling.
   */
  private LettuceClientConfiguration createPoolingClientConfiguration(
    ClientResources clientResources,
    ClientOptions clientOptions) {

    RedisConfigurationProperties.Pool poolConfig = properties.getLettuce().getPool();

    // Configure connection pool - use raw type to avoid generic mismatch
    @SuppressWarnings("rawtypes")
    GenericObjectPoolConfig poolConfiguration = new GenericObjectPoolConfig();

    if (poolConfig.getMaxActive() != null) {
      poolConfiguration.setMaxTotal(poolConfig.getMaxActive());
    }
    if (poolConfig.getMaxIdle() != null) {
      poolConfiguration.setMaxIdle(poolConfig.getMaxIdle());
    }
    if (poolConfig.getMinIdle() != null) {
      poolConfiguration.setMinIdle(poolConfig.getMinIdle());
    }
    if (poolConfig.getMaxWait() != null) {
      poolConfiguration.setMaxWait(Duration.ofMillis(poolConfig.getMaxWait()));
    }

    // Enable JMX monitoring
    poolConfiguration.setJmxEnabled(true);
    poolConfiguration.setJmxNamePrefix("redis-pool");

    // Enable test on borrow for connection validation
    poolConfiguration.setTestOnBorrow(true);
    poolConfiguration.setTestWhileIdle(true);

    log.debug("Pool config - maxActive: {}, maxIdle: {}, minIdle: {}, maxWait: {}",
      poolConfig.getMaxActive(), poolConfig.getMaxIdle(),
      poolConfig.getMinIdle(), poolConfig.getMaxWait());

    @SuppressWarnings("unchecked")
    LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
      LettucePoolingClientConfiguration.builder()
        .poolConfig(poolConfiguration)
        .clientResources(clientResources)
        .clientOptions(clientOptions);

    // Set command timeout if configured
    if (properties.getTimeout() != null && properties.getTimeout() > 0) {
      Duration timeout = Duration.ofMillis(properties.getTimeout());
      builder.commandTimeout(timeout);
      log.debug("Command timeout set to: {} ms", properties.getTimeout());
    }

    // Set shutdown timeout
    if (properties.getLettuce().getShutdownTimeout() != null) {
      builder.shutdownTimeout(Duration.ofMillis(properties.getLettuce().getShutdownTimeout()));
    }

    // Enable SSL if configured
    if (properties.isSsl()) {
      builder.useSsl();
      log.debug("SSL enabled for Redis connection");
    }

    return builder.build();
  }

  /**
   * Creates simple client configuration without pooling.
   */
  private LettuceClientConfiguration createSimpleClientConfiguration(
    ClientResources clientResources,
    ClientOptions clientOptions) {

    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
      LettuceClientConfiguration.builder()
        .clientResources(clientResources)
        .clientOptions(clientOptions);

    // Set command timeout if configured
    if (properties.getTimeout() != null && properties.getTimeout() > 0) {
      Duration timeout = Duration.ofMillis(properties.getTimeout());
      builder.commandTimeout(timeout);
      log.debug("Command timeout set to: {} ms", properties.getTimeout());
    }

    // Enable SSL if configured
    if (properties.isSsl()) {
      builder.useSsl();
      log.debug("SSL enabled for Redis connection");
    }

    return builder.build();
  }

  /**
   * Creates a RedisTemplate with proper serialization configured.
   * Uses String serializer for keys and JSON serializer for values.
   *
   * @param connectionFactory the Lettuce connection factory
   * @param objectMapper custom ObjectMapper with Java 8 date/time support
   * @return configured RedisTemplate
   */
  @Bean
  @Primary
  public RedisTemplate redisTemplate(LettuceConnectionFactory connectionFactory, @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {

    log.info("Configuring RedisTemplate");

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Configure serializers
    RedisSerializer<String> stringSerializer = new StringRedisSerializer();
    RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

    // Key serializers - always use String
    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);

    // Value serializers - use JSON
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    // Enable default serializer
    template.setDefaultSerializer(jsonSerializer);

    // Disable transaction support for better performance (enable if needed)
    template.setEnableTransactionSupport(false);

    // Enable expose connection for callbacks
    template.setExposeConnection(false);

    // Initialize the template
    template.afterPropertiesSet();

    log.info("RedisTemplate configured successfully with JSON serialization and Java 8 date/time support");
    return template;
  }

  /**
   * Creates a StringRedisTemplate for string-only operations.
   * More efficient than RedisTemplate when working with strings.
   *
   * @param connectionFactory the Lettuce connection factory
   * @return configured StringRedisTemplate
   */
  @Bean("stringRedisTemplate")
  public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
    log.info("Configuring StringRedisTemplate");

    StringRedisTemplate template = new StringRedisTemplate(connectionFactory);

    // Initialize the template
    template.afterPropertiesSet();

    log.info("StringRedisTemplate configured successfully");
    return template;
  }

  /**
   * Creates a RedisCommands bean for low-level Lettuce operations.
   * Required for RediSearch and other advanced Redis modules.
   *
   * <p>IMPORTANT: This creates a StatefulRedisConnection that should be reused.
   * The connection is thread-safe and can handle concurrent operations.
   *
   * @param connectionFactory the Lettuce connection factory
   * @return RedisCommands instance for sync operations
   */
  @Bean
  public RedisCommands<String, String> redisCommands(LettuceConnectionFactory connectionFactory) {
    log.info("Configuring RedisCommands for RediSearch operations");

    try {
      // Get the native Lettuce client
      Object nativeClient = connectionFactory.getNativeClient();

      if (!(nativeClient instanceof RedisClient)) {
        throw new IllegalStateException(
          "Native client is not a RedisClient. Type: " +
            (nativeClient != null ? nativeClient.getClass().getName() : "null")
        );
      }

      RedisClient client = (RedisClient) nativeClient;

      // Create a stateful connection (store it for cleanup)
      this.redisCommandsConnection = client.connect();

      // Return sync commands
      RedisCommands<String, String> commands = redisCommandsConnection.sync();

      log.info("RedisCommands configured successfully");
      return commands;

    } catch (Exception e) {
      log.error("Failed to create RedisCommands bean", e);
      throw new IllegalStateException("Failed to initialize Redis commands", e);
    }
  }

  /**
   * Cleanup method to close connections properly.
   */
  @PreDestroy
  public void cleanup() {
    log.info("Cleaning up Redis connections");

    if (redisCommandsConnection != null && redisCommandsConnection.isOpen()) {
      try {
        redisCommandsConnection.close();
        log.info("Closed RedisCommands connection");
      } catch (Exception e) {
        log.error("Error closing RedisCommands connection", e);
      }
    }

    log.info("Redis cleanup completed");
  }
}
