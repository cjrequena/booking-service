package com.cjrequena.sample.query.handler.service.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Fans booking updates out to every query-handler instance through Redis pub/sub, so an SSE client
 * hears about a change whichever instance projected it. Each instance's
 * {@link BookingUpdateSubscriber} delivers the message to its own connections.
 *
 * <p>Called right after a projection is saved. MongoDB writes are not transactional here, so the
 * new state is already readable; there is no commit to wait for.</p>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class BookingUpdatePublisher {

  public static final String CHANNEL = "booking-query-handler:sse:booking-updates";

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final BookingSSEService bookingSSEService;

  public void publish(BookingUpdateMessage update) {
    try {
      stringRedisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(update));
    } catch (JacksonException ex) {
      log.error("Failed to serialize booking update [bookingId={}]", update.bookingId(), ex);
    } catch (RuntimeException ex) {
      // Redis unreachable: reach this instance's own subscribers rather than nobody.
      log.warn("Failed to publish booking update to Redis, broadcasting locally [bookingId={}]: {}",
        update.bookingId(), ex.getMessage());
      bookingSSEService.broadcast(update);
    }
  }
}
