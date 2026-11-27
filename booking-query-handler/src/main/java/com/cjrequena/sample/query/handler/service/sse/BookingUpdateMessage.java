package com.cjrequena.sample.query.handler.service.sse;

/**
 * One "this booking changed, refetch it" hint, as carried between instances on
 * {@link BookingUpdatePublisher#CHANNEL} and pushed to SSE subscribers.
 *
 * @param bookingId the booking whose projection changed
 * @param status    the booking status after the change
 */
public record BookingUpdateMessage(String bookingId, String status) {
}
