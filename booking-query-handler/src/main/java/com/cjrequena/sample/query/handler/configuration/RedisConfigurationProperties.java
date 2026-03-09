package com.cjrequena.sample.query.handler.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * <p>
 * <p>
 * <p>
 *
 * @author cjrequena
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.redis")
public class RedisConfigurationProperties {

  /**
   * Connection URL. Overrides host, port, and password. User is ignored. Example:
   * redis://user:password@example.com:6379
   */
  private String url;

  /**
   * Redis server host.
   */
  private String host;

  /**
   * Login password of the redis server.
   */
  private String password;

  /**
   * Redis server port.
   */
  private int port;

  /**
   * Whether to enable SSL support.
   */
  private boolean ssl;

  /**
   * Connection timeout.
   */
  private Integer timeout;

  /**
   * Database index used by the connection factory.
   */
  private int database;

  private Sentinel sentinel;

  private Cluster cluster;

  private Lettuce lettuce;

  @Data
  public static class Pool {
    private Integer maxActive;
    private Integer maxIdle;
    private Integer maxWait;
    private Integer minIdle;
  }

  /**
   *
   */
  @Data
  public static class Sentinel {

    /**
     * Name of the Redis server.
     */
    private String master;

    /**
     * Comma-separated list of "host:port" pairs.
     */
    private List<String> nodes;
  }

  /**
   *
   */
  @Data
  public static class Cluster {

    /**
     * Comma-separated list of "host:port" pairs to bootstrap from. This represents an
     * "initial" list of cluster nodes and is required to have at least one entry.
     */
    private List<String> nodes;

    /**
     * Maximum number of redirects to follow when executing commands across the
     * cluster.
     */
    private Integer maxRedirects;
  }

  /**
   *
   */
  @Data
  public static class Lettuce {

    /**
     * Shutdown timeout.
     */
    private Integer shutdownTimeout;

    /**
     * Lettuce pool configuration.
     */
    private Pool pool;

  }

}
