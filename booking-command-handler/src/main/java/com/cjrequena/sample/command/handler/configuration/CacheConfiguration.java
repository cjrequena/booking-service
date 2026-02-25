package com.cjrequena.sample.command.handler.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for booking command projections.
 * <p>
 * Uses Caffeine as the caching provider for high-performance in-memory caching.
 * Configured caches:
 * <ul>
 *   <li>bookings: Individual booking lookups by ID (TTL: 10 minutes, max 1000 entries)</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

  /**
   * Configures the cache manager with Caffeine.
   * <p>
   * Cache Strategy:
   * - Maximum size: 1000 entries per cache
   * - TTL: 10 minutes after write
   * - Eviction: LRU (Least Recently Used)
   * - Async mode: Enabled for reactive support
   * </p>
   *
   * @return configured cache manager
   */
  @Bean
  public CacheManager caffeineCacheManager() {
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager("bookings");
    caffeineCacheManager.setCaffeine(caffeineCacheBuilder());
    caffeineCacheManager.setAsyncCacheMode(true); // Enable async mode for reactive support
    return caffeineCacheManager;
  }

  /**
   * Configures Caffeine cache builder with performance settings.
   *
   * @return Caffeine builder with configured settings
   */
  private Caffeine<Object, Object> caffeineCacheBuilder() {
    return Caffeine.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .recordStats(); // Enable cache statistics for monitoring
  }
}
