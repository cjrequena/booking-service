package com.cjrequena.sample.command.handler.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaxVO Tests")
class PaxVOTest {

  @Test
  @DisplayName("Should create PaxVO with all required fields")
  void shouldCreatePaxVOWithAllFields() {
    UUID paxId = UUID.randomUUID();

    PaxVO pax = PaxVO.builder()
      .paxId(paxId)
      .firstName("John")
      .lastName("Doe")
      .email("john.doe@example.com")
      .phone("+1234567890")
      .age(30)
      .documentType("PASSPORT")
      .documentNumber("AB123456")
      .paxType("ADULT")
      .build();

    assertNotNull(pax);
    assertEquals(paxId, pax.paxId());
    assertEquals("John", pax.firstName());
    assertEquals("Doe", pax.lastName());
    assertEquals("john.doe@example.com", pax.email());
    assertEquals("+1234567890", pax.phone());
    assertEquals(30, pax.age());
    assertEquals("PASSPORT", pax.documentType());
    assertEquals("AB123456", pax.documentNumber());
    assertEquals("ADULT", pax.paxType());
  }

  @Test
  @DisplayName("Should create PaxVO for child passenger")
  void shouldCreateChildPaxVO() {
    PaxVO child = PaxVO.builder()
      .paxId(UUID.randomUUID())
      .firstName("Alice")
      .lastName("Smith")
      .email("parent@example.com")
      .phone("+1234567890")
      .age(8)
      .documentType("PASSPORT")
      .documentNumber("CH123456")
      .paxType("CHILD")
      .build();

    assertEquals("CHILD", child.paxType());
    assertEquals(8, child.age());
  }

  @Test
  @DisplayName("Should create PaxVO for infant passenger")
  void shouldCreateInfantPaxVO() {
    PaxVO infant = PaxVO.builder()
      .paxId(UUID.randomUUID())
      .firstName("Baby")
      .lastName("Johnson")
      .email("parent@example.com")
      .phone("+1234567890")
      .age(1)
      .documentType("PASSPORT")
      .documentNumber("IN123456")
      .paxType("INFANT")
      .build();

    assertEquals("INFANT", infant.paxType());
    assertEquals(1, infant.age());
  }

  @Test
  @DisplayName("Should support different document types")
  void shouldSupportDifferentDocumentTypes() {
    PaxVO paxWithId = PaxVO.builder()
      .paxId(UUID.randomUUID())
      .firstName("Jane")
      .lastName("Doe")
      .email("jane@example.com")
      .phone("+1234567890")
      .age(25)
      .documentType("ID_CARD")
      .documentNumber("ID987654")
      .paxType("ADULT")
      .build();

    assertEquals("ID_CARD", paxWithId.documentType());
    assertEquals("ID987654", paxWithId.documentNumber());
  }

  @Test
  @DisplayName("Should be immutable - record properties cannot be changed")
  void shouldBeImmutable() {
    UUID paxId = UUID.randomUUID();
    PaxVO pax = PaxVO.builder()
      .paxId(paxId)
      .firstName("John")
      .lastName("Doe")
      .email("john@example.com")
      .phone("+1234567890")
      .age(30)
      .documentType("PASSPORT")
      .documentNumber("AB123456")
      .paxType("ADULT")
      .build();

    // Records are immutable by design
    assertEquals(paxId, pax.paxId());
    assertEquals("John", pax.firstName());
  }

  @Test
  @DisplayName("Should support equality comparison")
  void shouldSupportEqualityComparison() {
    UUID paxId = UUID.randomUUID();

    PaxVO pax1 = PaxVO.builder()
      .paxId(paxId)
      .firstName("John")
      .lastName("Doe")
      .email("john@example.com")
      .phone("+1234567890")
      .age(30)
      .documentType("PASSPORT")
      .documentNumber("AB123456")
      .paxType("ADULT")
      .build();

    PaxVO pax2 = PaxVO.builder()
      .paxId(paxId)
      .firstName("John")
      .lastName("Doe")
      .email("john@example.com")
      .phone("+1234567890")
      .age(30)
      .documentType("PASSPORT")
      .documentNumber("AB123456")
      .paxType("ADULT")
      .build();

    assertEquals(pax1, pax2);
    assertEquals(pax1.hashCode(), pax2.hashCode());
  }

  @Test
  @DisplayName("Should not be equal when fields differ")
  void shouldNotBeEqualWhenFieldsDiffer() {
    PaxVO pax1 = PaxVO.builder()
      .paxId(UUID.randomUUID())
      .firstName("John")
      .lastName("Doe")
      .email("john@example.com")
      .phone("+1234567890")
      .age(30)
      .documentType("PASSPORT")
      .documentNumber("AB123456")
      .paxType("ADULT")
      .build();

    PaxVO pax2 = PaxVO.builder()
      .paxId(UUID.randomUUID())
      .firstName("Jane")
      .lastName("Doe")
      .email("jane@example.com")
      .phone("+1234567891")
      .age(28)
      .documentType("PASSPORT")
      .documentNumber("CD789012")
      .paxType("ADULT")
      .build();

    assertNotEquals(pax1, pax2);
  }

  @Test
  @DisplayName("Should have meaningful toString representation")
  void shouldHaveMeaningfulToString() {
    PaxVO pax = PaxVO.builder()
      .paxId(UUID.randomUUID())
      .firstName("John")
      .lastName("Doe")
      .email("john@example.com")
      .phone("+1234567890")
      .age(30)
      .documentType("PASSPORT")
      .documentNumber("AB123456")
      .paxType("ADULT")
      .build();

    String toString = pax.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("John"));
    assertTrue(toString.contains("Doe"));
  }
}
