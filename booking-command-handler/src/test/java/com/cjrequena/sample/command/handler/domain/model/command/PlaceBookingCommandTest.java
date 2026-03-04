package com.cjrequena.sample.command.handler.domain.model.command;

import com.cjrequena.sample.command.handler.domain.model.vo.PaxVO;
import com.cjrequena.sample.command.handler.domain.model.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlaceBookingCommand Tests")
class PlaceBookingCommandTest {

  private List<PaxVO> paxes;
  private UUID leadPaxId;
  private List<ProductVO> products;

  @BeforeEach
  void setUp() {
    leadPaxId = UUID.randomUUID();
    paxes = List.of(
      PaxVO.builder()
        .paxId(leadPaxId)
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .phone("+1234567890")
        .age(30)
        .documentType("PASSPORT")
        .documentNumber("AB123456")
        .paxType("ADULT")
        .build()
    );
    products = List.of();
  }

  @Test
  @DisplayName("Should create command with all required fields")
  void shouldCreateCommandWithAllFields() {
    PlaceBookingCommand command = PlaceBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .build();

    assertNotNull(command);
    assertNotNull(command.getAggregateId());
    assertNotNull(command.getBookingReference());
    assertEquals(paxes, command.getPaxes());
    assertEquals(leadPaxId, command.getLeadPaxId());
    assertEquals(products, command.getProducts());
  }

  @Test
  @DisplayName("Should generate unique booking reference")
  void shouldGenerateUniqueBookingReference() {
    PlaceBookingCommand command1 = PlaceBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .build();

    PlaceBookingCommand command2 = PlaceBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .build();

    assertNotEquals(command1.getBookingReference(), command2.getBookingReference());
  }

  @Test
  @DisplayName("Should generate unique aggregate ID")
  void shouldGenerateUniqueAggregateId() {
    PlaceBookingCommand command1 = PlaceBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .build();

    PlaceBookingCommand command2 = PlaceBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .build();

    assertNotEquals(command1.getAggregateId(), command2.getAggregateId());
  }

  @Test
  @DisplayName("Should create command with multiple passengers")
  void shouldCreateCommandWithMultiplePassengers() {
    List<PaxVO> multiplePaxes = List.of(
      PaxVO.builder()
        .paxId(leadPaxId)
        .firstName("John")
        .lastName("Doe")
        .email("john@example.com")
        .phone("+1234567890")
        .age(30)
        .documentType("PASSPORT")
        .documentNumber("AB123456")
        .paxType("ADULT")
        .build(),
      PaxVO.builder()
        .paxId(UUID.randomUUID())
        .firstName("Jane")
        .lastName("Doe")
        .email("jane@example.com")
        .phone("+1234567891")
        .age(28)
        .documentType("PASSPORT")
        .documentNumber("CD789012")
        .paxType("ADULT")
        .build()
    );

    PlaceBookingCommand command = PlaceBookingCommand.builder()
      .paxes(multiplePaxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .build();

    assertEquals(2, command.getPaxes().size());
  }

  @Test
  @DisplayName("Should have aggregate type set")
  void shouldHaveAggregateTypeSet() {
    PlaceBookingCommand command = PlaceBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .build();

    assertNotNull(command.getAggregateType());
    assertFalse(command.getAggregateType().isEmpty());
  }

  @Test
  @DisplayName("Should have meaningful toString representation")
  void shouldHaveMeaningfulToString() {
    PlaceBookingCommand command = PlaceBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .build();

    String toString = command.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("PlaceBookingCommand"));
  }
}
