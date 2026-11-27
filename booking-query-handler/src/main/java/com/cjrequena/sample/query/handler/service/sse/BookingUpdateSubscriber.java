package com.cjrequena.sample.query.handler.service.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Receives booking updates published on {@link BookingUpdatePublisher#CHANNEL} and pushes them to this instance's SSE clients. */
@Log4j2
@Component
@RequiredArgsConstructor
public class BookingUpdateSubscriber implements MessageListener {

  private final ObjectMapper objectMapper;
  private final BookingSSEService bookingSSEService;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    BookingUpdateMessage update;
    try {
      update = objectMapper.readValue(message.getBody(), BookingUpdateMessage.class);
    } catch (JacksonException ex) {
      log.warn("Malformed booking update skipped: {}", ex.getMessage());
      return;
    }
    if (update.bookingId() == null) {
      log.warn("Booking update without bookingId skipped");
      return;
    }
    bookingSSEService.broadcast(update);
  }
}
