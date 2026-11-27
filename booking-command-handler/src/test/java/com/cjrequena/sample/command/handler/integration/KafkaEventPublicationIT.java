package com.cjrequena.sample.command.handler.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the outbound integration path end to end: a command persists an event, the event store
 * subscription poller picks it up, and {@code KafkaEventEmitter} publishes it as a binary-mode
 * CloudEvent keyed by aggregate id, with no framework headers on the wire.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationTest")
class KafkaEventPublicationIT {

  private static final String ACCEPT_VERSION_HEADER = "Accept-Version";
  private static final String ACCEPT_VERSION_VALUE = "application/vnd.booking-command-handler.v1";
  private static final String TOPIC = "event-booking-01";

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
    .withDatabaseName("eventstore")
    .withUsername("postgres")
    .withPassword("postgres");

  @Container
  static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:latest"));

  @Container
  static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));

  @LocalServerPort
  private int port;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.eventstore.jdbcUrl", postgres::getJdbcUrl);
    registry.add("spring.datasource.eventstore.username", postgres::getUsername);
    registry.add("spring.datasource.eventstore.password", postgres::getPassword);
    registry.add("spring.mongodb.uri", mongo::getReplicaSetUrl);
    registry.add(
      "spring.cloud.stream.binders.my-kafka-binder-01.environment.spring.cloud.stream.kafka.binder.brokers",
      kafka::getBootstrapServers
    );
    // Turn the event store subscription on, polling fast, so events flow to Kafka.
    registry.add("eventstore.subscription.enabled", () -> "true");
    registry.add("eventstore.subscription.name", () -> "kafka-event-publication-it");
    registry.add("eventstore.subscription.polling-initial-delay", () -> "PT1S");
    registry.add("eventstore.subscription.polling-interval", () -> "PT1S");
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    RestAssured.basePath = "/command-handler/api/bookings";
  }

  @Test
  @DisplayName("BookingCreatedEvent is published to Kafka as a CloudEvent keyed by booking id")
  void shouldPublishCreatedEventAsCloudEvent() {
    String payload = """
      {
        "paxes": [{
          "pax_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "first_name": "John",
          "last_name": "Doe",
          "email": "john@example.com",
          "phone": "+34600000000",
          "age": 30,
          "document_type": "PASSPORT",
          "document_number": "AB123456",
          "pax_type": "ADULT"
        }],
        "lead_pax_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "products": [
          {
            "product_type": "Transfer",
            "product_id": "11111111-1111-1111-1111-111111111111",
            "search_id": "aaaa1111-bbbb-cccc-dddd-eeee11111111",
            "search_created_at": "2026-06-01T08:00:00.000+00:00",
            "paxes_ids": ["a1b2c3d4-e5f6-7890-abcd-ef1234567890"],
            "origin": { "latitude": 39.55, "longitude": 2.73, "iata_code": "PMI", "full_address": "Palma Airport" },
            "destination": { "latitude": 39.69, "longitude": 3.01, "full_address": "Hotel Grand Mallorca" },
            "outbound_trip": {
              "trip_id": "aa000001-0001-0001-0001-000000000001",
              "pickup_datetime": "2026-06-15T10:30:00.000+00:00",
              "transfer_type": "ONE_WAY",
              "vehicle": {
                "vehicle_id": "bb000001-0001-0001-0001-000000000001",
                "type": "SEDAN", "description": "Mercedes E-Class", "model": "Mercedes E-Class",
                "capacity": 4, "max_bags": 3, "max_paxes": 3
              }
            },
            "price": { "service_type": "PRIVATE", "currency": "EUR", "total_amount": 65.00, "subtotal_amount": 55.00, "fees_and_taxes": 10.00 }
          },
          {
            "product_type": "Hotel",
            "product_id": "22222222-2222-2222-2222-222222222222",
            "search_id": "aaaa1111-bbbb-cccc-dddd-eeee11111111",
            "search_created_at": "2026-06-01T08:00:00.000+00:00",
            "paxes_ids": ["a1b2c3d4-e5f6-7890-abcd-ef1234567890"],
            "hotel_name": "Hotel Grand Mallorca",
            "hotel_code": "HGM-001",
            "location": { "latitude": 39.69, "longitude": 3.01, "full_address": "Av. de Sa Coma 12, Cala Millor" },
            "check_in": "2026-06-15T14:00:00.000+00:00",
            "check_out": "2026-06-20T11:00:00.000+00:00",
            "rooms": [{ "room_id": "cc000001-0001-0001-0001-000000000001", "room_type": "DOUBLE_SEA_VIEW", "room_description": "Double room with sea view", "quantity": 1, "max_occupancy": 2 }],
            "price": { "currency": "EUR", "total_amount": 875.00, "subtotal_amount": 750.00, "fees_and_taxes": 125.00, "nightly_rate": 150.00, "nights": 5 }
          }
        ],
        "metadata": { "source": "web" }
      }
      """;

    String bookingId = given()
      .header(ACCEPT_VERSION_HEADER, ACCEPT_VERSION_VALUE)
      .contentType(ContentType.JSON)
      .body(payload)
      .when()
      .post("/create")
      .then()
      .statusCode(201)
      .extract().jsonPath().getString("booking_id");

    ConsumerRecord<String, String> record = awaitRecord(bookingId, Duration.ofSeconds(30))
      .orElseThrow(() -> new AssertionError("No Kafka record for booking " + bookingId + " on " + TOPIC));

    assertThat(header(record, "ce_specversion")).isEqualTo("1.0");
    assertThat(header(record, "ce_type")).isEqualTo("BookingCreatedEvent");
    assertThat(header(record, "ce_source")).isEqualTo("/booking-command-handler");
    assertThat(header(record, "ce_id")).isNotBlank();
    assertThat(header(record, "ce_time")).isNotBlank();
    assertThat(header(record, "content-type")).isEqualTo("application/json");
    // CloudEvents-clean: no Spring framework headers leak onto the wire.
    assertThat(record.headers().lastHeader("spring_json_header_types")).isNull();
    assertThat(record.headers().lastHeader("message-type")).isNull();
    assertThat(record.value()).contains(bookingId);
  }

  private static Optional<ConsumerRecord<String, String>> awaitRecord(String key, Duration timeout) {
    Map<String, Object> config = Map.of(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
      ConsumerConfig.GROUP_ID_CONFIG, "kafka-event-publication-it",
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
    );
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
      consumer.subscribe(List.of(TOPIC));
      Instant deadline = Instant.now().plus(timeout);
      while (Instant.now().isBefore(deadline)) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
          if (key.equals(record.key())) {
            return Optional.of(record);
          }
        }
      }
    }
    return Optional.empty();
  }

  private static String header(ConsumerRecord<?, ?> record, String name) {
    Header header = record.headers().lastHeader(name);
    return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
  }
}
