package com.cjrequena.sample.query.handler.service.sse;

import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Holds this instance's SSE connections and pushes booking updates to them.
 *
 * <p>A client subscribes either to every booking or to a single one. Connections are kept alive
 * with a comment heartbeat so proxies don't time them out, and dead connections are pruned
 * whenever a send fails.</p>
 */
@Service
@Log4j2
public class BookingSSEService {

  static final String ALL_BOOKINGS = "*";
  static final String EVENT_NAME = "booking-update";
  private static final long HEARTBEAT_SECONDS = 15;

  private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
  private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "sse-heartbeat");
    t.setDaemon(true);
    return t;
  });

  public BookingSSEService() {
    heartbeat.scheduleAtFixedRate(this::sendHeartbeats, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
  }

  /**
   * @param bookingId the booking to follow, or {@code null} to follow every booking
   */
  public SseEmitter subscribe(UUID bookingId) {
    String key = bookingId == null ? ALL_BOOKINGS : bookingId.toString();
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
    Runnable cleanup = () -> removeEmitter(key, emitter);
    emitter.onCompletion(cleanup);
    emitter.onTimeout(cleanup);
    emitter.onError(e -> cleanup.run());
    // Flush response headers immediately so clients and proxies don't wait for the first event.
    try {
      emitter.send(SseEmitter.event().comment("connected"));
    } catch (IOException e) {
      removeEmitter(key, emitter);
      emitter.completeWithError(e);
      return emitter;
    }
    log.debug("SSE client subscribed: key={}", key);
    return emitter;
  }

  /** Pushes an update to the clients following every booking and to those following this one. */
  public void broadcast(BookingUpdateMessage update) {
    int delivered = send(ALL_BOOKINGS, update) + send(update.bookingId(), update);
    log.debug("SSE broadcast: bookingId={}, status={}, clients={}", update.bookingId(), update.status(), delivered);
  }

  private int send(String key, BookingUpdateMessage update) {
    List<SseEmitter> connected = emitters.getOrDefault(key, List.of());
    if (connected.isEmpty()) {
      return 0;
    }
    List<SseEmitter> dead = new ArrayList<>();
    for (SseEmitter emitter : connected) {
      try {
        emitter.send(SseEmitter.event()
          .name(EVENT_NAME)
          .id(update.bookingId())
          .data(Map.of("booking_id", update.bookingId(), "status", update.status())));
      } catch (IOException | IllegalStateException e) {
        dead.add(emitter);
      }
    }
    connected.removeAll(dead);
    return connected.size();
  }

  private void sendHeartbeats() {
    for (List<SseEmitter> connected : emitters.values()) {
      List<SseEmitter> dead = new ArrayList<>();
      for (SseEmitter emitter : connected) {
        try {
          emitter.send(SseEmitter.event().comment("keepalive"));
        } catch (IOException | IllegalStateException e) {
          dead.add(emitter);
        }
      }
      connected.removeAll(dead);
    }
  }

  private void removeEmitter(String key, SseEmitter emitter) {
    List<SseEmitter> list = emitters.get(key);
    if (list != null) {
      list.remove(emitter);
    }
  }

  @PreDestroy
  public void shutdown() {
    heartbeat.shutdownNow();
  }
}
