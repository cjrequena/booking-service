package com.cjrequena.sample.query.handler.configuration;

import com.cjrequena.sample.query.handler.shared.common.converter.BinaryToUUIDConverter;
import com.cjrequena.sample.query.handler.shared.common.converter.OffsetDateTimeToStringConverter;
import com.cjrequena.sample.query.handler.shared.common.converter.StringToOffsetDateTimeConverter;
import com.cjrequena.sample.query.handler.shared.common.converter.UUIDToBinaryConverter;
import com.mongodb.MongoCompressor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.mongodb")
@RequiredArgsConstructor(onConstructor=@__(@Autowired))
public class ProjectionDBMongoConfiguration {

  private final MeterRegistry meterRegistry;
  private String database;
  private String uri;

  @Bean
  public MongoCustomConversions customConversions() {
    List<Object> converters = new ArrayList<>();
    converters.add(new StringToOffsetDateTimeConverter());
    converters.add(new OffsetDateTimeToStringConverter());
    converters.add(new BinaryToUUIDConverter());
    converters.add(new UUIDToBinaryConverter());
    return new MongoCustomConversions(converters);
  }

  @Bean
  public MongoClientSettingsBuilderCustomizer mongoClientSettingsBuilderCustomizer() {
    return settings -> {
      settings.compressorList(List.of(MongoCompressor.createZlibCompressor()));
      settings.addCommandListener(new MongoMetricsCommandListener(meterRegistry));
      settings.uuidRepresentation(UuidRepresentation.STANDARD);
    };
  }

//  @Bean
//  public MongoClient mongoClient() {
//    return MongoClients.create(uri);
//  }
//
//  @Bean
//  public MongoTemplate mongoTemplate() {
//    return new MongoTemplate(mongoClient(), database);
//  }

}
