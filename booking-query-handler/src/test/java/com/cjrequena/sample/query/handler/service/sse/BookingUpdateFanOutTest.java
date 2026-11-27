package com.cjrequena.sample.query.handler.service.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BookingUpdateFanOutTest {

  private final ObjectMapper objectMapper = JsonMapper.builder().build();
  private final BookingUpdateMessage update = new BookingUpdateMessage("b-1", "CONFIRMED");

  private StringRedisTemplate redis;
  private BookingSSEService sseService;
  private BookingUpdatePublisher publisher;
  private BookingUpdateSubscriber subscriber;

  @BeforeEach
  void setUp() {
    redis = mock(StringRedisTemplate.class);
    sseService = mock(BookingSSEService.class);
    publisher = new BookingUpdatePublisher(redis, objectMapper, sseService);
    subscriber = new BookingUpdateSubscriber(objectMapper, sseService);
  }

  @Test
  void publishesToTheRedisChannel() {
    publisher.publish(update);
    verify(redis).convertAndSend(eq(BookingUpdatePublisher.CHANNEL), anyString());
    verify(sseService, never()).broadcast(any());
  }

  @Test
  void fallsBackToLocalBroadcastWhenRedisIsDown() {
    doThrow(new RedisConnectionFailureException("down")).when(redis).convertAndSend(anyString(), anyString());
    publisher.publish(update);
    verify(sseService).broadcast(update);
  }

  @Test
  void subscriberRoundTripsThePublishedMessage() {
    String json = objectMapper.writeValueAsString(update);
    subscriber.onMessage(new DefaultMessage(BookingUpdatePublisher.CHANNEL.getBytes(), json.getBytes(StandardCharsets.UTF_8)), null);
    verify(sseService).broadcast(update);
  }

  @Test
  void subscriberSkipsMalformedOrIncompleteMessages() {
    subscriber.onMessage(new DefaultMessage(new byte[0], "not json".getBytes(StandardCharsets.UTF_8)), null);
    subscriber.onMessage(new DefaultMessage(new byte[0], "{\"status\":\"CONFIRMED\"}".getBytes(StandardCharsets.UTF_8)), null);
    verify(sseService, never()).broadcast(any());
  }

  @Test
  void sseServiceAcceptsSubscribersAndBroadcasts() {
    BookingSSEService real = new BookingSSEService();
    try {
      assertThat(real.subscribe(null)).isNotNull();
      assertThat(real.subscribe(java.util.UUID.randomUUID())).isNotNull();
      real.broadcast(update);
    } finally {
      real.shutdown();
    }
  }
}
