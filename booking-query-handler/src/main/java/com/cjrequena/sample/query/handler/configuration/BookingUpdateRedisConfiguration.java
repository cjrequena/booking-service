package com.cjrequena.sample.query.handler.configuration;

import com.cjrequena.sample.query.handler.service.sse.BookingUpdatePublisher;
import com.cjrequena.sample.query.handler.service.sse.BookingUpdateSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/** Subscribes this instance to the booking-update channel that feeds its SSE clients. */
@Configuration
public class BookingUpdateRedisConfiguration {

  @Bean
  public RedisMessageListenerContainer bookingUpdateListenerContainer(
    LettuceConnectionFactory connectionFactory,
    BookingUpdateSubscriber bookingUpdateSubscriber
  ) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(bookingUpdateSubscriber, new ChannelTopic(BookingUpdatePublisher.CHANNEL));
    return container;
  }
}
