package com.cjrequena.sample.command.handler.shared.common;

/**
 * <p>
 * <p>
 * <p>
 * <p>
 *
 * @author cjrequena
 */
public class Constant {
  /** */
  public static final String VND_BOOKING_COMMAND_HANDLER_V1 = "application/vnd.booking-command-handler.v1";
  public static final String ISO_LOCAL_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss";       // no offset
  public static final String ISO_OFFSET_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX"; // date + time + offset
  public static final String ISO_DATE = "yyyy-MM-dd";
  /**
   * PRODUCTS */
  public static final String TRANSFER = "Transfer";
  public static final String HOTEL = "Hotel";

  public static final String KAFKA_BOOKING_ORDER_OUTBOUND_CHANNEL = "event-booking-out-0";


}
