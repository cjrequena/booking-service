package com.cjrequena.sample.command.handler.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Full-stack integration test using Testcontainers.
 * <p>
 * Boots PostgreSQL (Event Store), MongoDB (Projections), and Kafka,
 * then exercises the complete booking lifecycle including multi-product
 * bookings with Transfer + Hotel.
 * </p>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationTest")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingCommandHandlerIT {

  private static final String ACCEPT_VERSION_HEADER = "Accept-Version";
  private static final String ACCEPT_VERSION_VALUE = "application/vnd.booking-command-handler.v1";

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
    .withDatabaseName("eventstore")
    .withUsername("postgres")
    .withPassword("postgres")
    .withInitScript("init-eventstore.sql");

  @Container
  static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:latest"));

  @Container
  static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));

  @LocalServerPort
  private int port;

  private static String bookingId;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL
    registry.add("spring.datasource.eventstore.jdbcUrl", postgres::getJdbcUrl);
    registry.add("spring.datasource.eventstore.username", postgres::getUsername);
    registry.add("spring.datasource.eventstore.password", postgres::getPassword);

    // MongoDB
    registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);

    // Kafka
    registry.add(
      "spring.cloud.stream.binders.my-kafka-binder-01.environment.spring.cloud.stream.kafka.binder.brokers",
      kafka::getBootstrapServers
    );
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    RestAssured.basePath = "/command-handler/api/bookings";
  }

  @Test
  @Order(1)
  @DisplayName("Create booking with Transfer + Hotel products")
  void shouldCreateMultiProductBooking() {
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

    Response response = given()
      .header(ACCEPT_VERSION_HEADER, ACCEPT_VERSION_VALUE)
      .contentType(ContentType.JSON)
      .body(payload)
      .when()
      .post("/create")
      .then()
      .statusCode(201)
      .body("booking_id", notNullValue())
      .body("status", equalTo("CREATED"))
      .extract().response();

    bookingId = response.jsonPath().getString("booking_id");
  }

  @Test
  @Order(2)
  @DisplayName("Confirm the booking")
  void shouldConfirmBooking() {
    given()
      .header(ACCEPT_VERSION_HEADER, ACCEPT_VERSION_VALUE)
      .contentType(ContentType.JSON)
      .when()
      .post("/{bookingId}/confirm", bookingId)
      .then()
      .statusCode(200)
      .body("booking_id", equalTo(bookingId))
      .body("status", equalTo("CONFIRMED"));
  }

  @Test
  @Order(3)
  @DisplayName("Complete the booking")
  void shouldCompleteBooking() {
    given()
      .header(ACCEPT_VERSION_HEADER, ACCEPT_VERSION_VALUE)
      .contentType(ContentType.JSON)
      .when()
      .post("/{bookingId}/complete", bookingId)
      .then()
      .statusCode(200)
      .body("booking_id", equalTo(bookingId))
      .body("status", equalTo("COMPLETED"));
  }

  @Test
  @Order(4)
  @DisplayName("Create and cancel a booking")
  void shouldCreateAndCancelBooking() {
    String payload = """
      {
        "paxes": [{
          "pax_id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
          "first_name": "Jane",
          "last_name": "Doe",
          "email": "jane@example.com",
          "phone": "+34600000001",
          "age": 28,
          "document_type": "PASSPORT",
          "document_number": "CD789012",
          "pax_type": "ADULT"
        }],
        "lead_pax_id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
        "products": [{
          "product_type": "Hotel",
          "product_id": "33333333-3333-3333-3333-333333333333",
          "search_id": "bbbb2222-cccc-dddd-eeee-ffff22222222",
          "search_created_at": "2026-07-01T08:00:00.000+00:00",
          "paxes_ids": ["d1e2f3a4-b5c6-7890-abcd-ef1234567890"],
          "hotel_name": "Beach Resort",
          "hotel_code": "BR-002",
          "location": { "latitude": 40.0, "longitude": 3.5, "full_address": "Beach Road 1" },
          "check_in": "2026-07-10T14:00:00.000+00:00",
          "check_out": "2026-07-13T11:00:00.000+00:00",
          "rooms": [{ "room_id": "dd000001-0001-0001-0001-000000000001", "room_type": "SINGLE", "quantity": 1, "max_occupancy": 1 }],
          "price": { "currency": "EUR", "total_amount": 450.00, "subtotal_amount": 400.00, "fees_and_taxes": 50.00, "nightly_rate": 133.33, "nights": 3 }
        }],
        "metadata": { "source": "mobile" }
      }
      """;

    Response response = given()
      .header(ACCEPT_VERSION_HEADER, ACCEPT_VERSION_VALUE)
      .contentType(ContentType.JSON)
      .body(payload)
      .when()
      .post("/create")
      .then()
      .statusCode(201)
      .body("status", equalTo("CREATED"))
      .extract().response();

    String cancelBookingId = response.jsonPath().getString("booking_id");

    given()
      .header(ACCEPT_VERSION_HEADER, ACCEPT_VERSION_VALUE)
      .contentType(ContentType.JSON)
      .when()
      .post("/{bookingId}/cancel", cancelBookingId)
      .then()
      .statusCode(200)
      .body("booking_id", equalTo(cancelBookingId))
      .body("status", equalTo("CANCELLED"));
  }
}
