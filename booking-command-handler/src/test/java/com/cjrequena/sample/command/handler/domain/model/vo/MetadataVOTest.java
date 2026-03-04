package com.cjrequena.sample.command.handler.domain.model.vo;

import com.cjrequena.sample.command.handler.TestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetadataVO Tests")
class MetadataVOTest extends TestBase {

  @Test
  @DisplayName("Should create empty metadata")
  void shouldCreateEmptyMetadata() {
    MetadataVO metadata = MetadataVO.empty();

    assertNotNull(metadata);
    assertTrue(metadata.isEmpty());
    assertEquals(0, metadata.size());
  }

  @Test
  @DisplayName("Should create metadata from Map")
  void shouldCreateMetadataFromMap() {
    Map<String, Object> map = Map.of(
      "key1", "value1",
      "key2", 123,
      "key3", true
    );

    MetadataVO metadata = MetadataVO.of(map);

    assertFalse(metadata.isEmpty());
    assertEquals(3, metadata.size());
    assertEquals("value1", metadata.getString("key1"));
    assertEquals(123, metadata.getInt("key2"));
    assertTrue(metadata.getBoolean("key3"));
  }

  @Test
  @DisplayName("Should create metadata from JSON string")
  void shouldCreateMetadataFromJsonString() {
    String json = "{\"name\":\"test\",\"count\":42}";

    MetadataVO metadata = MetadataVO.of(json);

    assertFalse(metadata.isEmpty());
    assertEquals("test", metadata.getString("name"));
    assertEquals(42, metadata.getInt("count"));
  }

  @Test
  @DisplayName("Should handle invalid JSON string gracefully")
  void shouldHandleInvalidJsonString() {
    assertThrows(IllegalArgumentException.class, () -> {
      MetadataVO.of("invalid json");
    });
  }

  @Test
  @DisplayName("Should add and retrieve string values")
  void shouldAddAndRetrieveStringValues() {
    MetadataVO metadata = MetadataVO.empty()
      .with("key1", "value1")
      .with("key2", "value2");

    assertEquals("value1", metadata.getString("key1"));
    assertEquals("value2", metadata.getString("key2"));
    assertNull(metadata.getString("nonexistent"));
  }

  @Test
  @DisplayName("Should add and retrieve integer values")
  void shouldAddAndRetrieveIntegerValues() {
    MetadataVO metadata = MetadataVO.empty()
      .withInt("count", 42)
      .withInt("total", 100);

    assertEquals(42, metadata.getInt("count"));
    assertEquals(100, metadata.getInt("total"));
    assertNull(metadata.getInt("nonexistent"));
  }

  @Test
  @DisplayName("Should add and retrieve long values")
  void shouldAddAndRetrieveLongValues() {
    MetadataVO metadata = MetadataVO.empty()
      .withLong("timestamp", 1234567890L);

    assertEquals(1234567890L, metadata.getLong("timestamp"));
  }

  @Test
  @DisplayName("Should add and retrieve double values")
  void shouldAddAndRetrieveDoubleValues() {
    MetadataVO metadata = MetadataVO.empty()
      .withDouble("price", 99.99);

    assertEquals(99.99, metadata.getDouble("price"));
  }

  @Test
  @DisplayName("Should add and retrieve boolean values")
  void shouldAddAndRetrieveBooleanValues() {
    MetadataVO metadata = MetadataVO.empty()
      .withBoolean("active", true)
      .withBoolean("deleted", false);

    assertTrue(metadata.getBoolean("active"));
    assertFalse(metadata.getBoolean("deleted"));
  }

  @Test
  @DisplayName("Should add and retrieve string lists")
  void shouldAddAndRetrieveStringLists() {
    List<String> tags = Arrays.asList("tag1", "tag2", "tag3");
    MetadataVO metadata = MetadataVO.empty()
      .withStringList("tags", tags);

    List<String> retrievedTags = metadata.getStringList("tags");
    assertEquals(3, retrievedTags.size());
    assertTrue(retrievedTags.containsAll(tags));
  }

  @Test
  @DisplayName("Should add and retrieve nested objects")
  void shouldAddAndRetrieveNestedObjects() {
    MetadataVO nested = MetadataVO.empty()
      .with("nestedKey", "nestedValue");

    MetadataVO metadata = MetadataVO.empty()
      .withObject("nested", nested);

    MetadataVO retrievedNested = metadata.getObject("nested");
    assertEquals("nestedValue", retrievedNested.getString("nestedKey"));
  }

  @Test
  @DisplayName("Should check if key exists")
  void shouldCheckIfKeyExists() {
    MetadataVO metadata = MetadataVO.empty()
      .with("existingKey", "value");

    assertTrue(metadata.has("existingKey"));
    assertFalse(metadata.has("nonexistentKey"));
  }

  @Test
  @DisplayName("Should get all keys")
  void shouldGetAllKeys() {
    MetadataVO metadata = MetadataVO.empty()
      .with("key1", "value1")
      .with("key2", "value2")
      .withInt("key3", 123);

    assertEquals(3, metadata.keys().size());
    assertTrue(metadata.keys().contains("key1"));
    assertTrue(metadata.keys().contains("key2"));
    assertTrue(metadata.keys().contains("key3"));
  }

  @Test
  @DisplayName("Should remove single key")
  void shouldRemoveSingleKey() {
    MetadataVO metadata = MetadataVO.empty()
      .with("key1", "value1")
      .with("key2", "value2");

    MetadataVO updated = metadata.without("key1");

    assertFalse(updated.has("key1"));
    assertTrue(updated.has("key2"));
  }

  @Test
  @DisplayName("Should remove multiple keys")
  void shouldRemoveMultipleKeys() {
    MetadataVO metadata = MetadataVO.empty()
      .with("key1", "value1")
      .with("key2", "value2")
      .with("key3", "value3");

    MetadataVO updated = metadata.without("key1", "key2");

    assertFalse(updated.has("key1"));
    assertFalse(updated.has("key2"));
    assertTrue(updated.has("key3"));
  }

  @Test
  @DisplayName("Should merge two metadata objects")
  void shouldMergeTwoMetadataObjects() {
    MetadataVO metadata1 = MetadataVO.empty()
      .with("key1", "value1")
      .with("key2", "value2");

    MetadataVO metadata2 = MetadataVO.empty()
      .with("key2", "updatedValue2")
      .with("key3", "value3");

    MetadataVO merged = metadata1.merge(metadata2);

    assertEquals("value1", merged.getString("key1"));
    assertEquals("updatedValue2", merged.getString("key2"));
    assertEquals("value3", merged.getString("key3"));
  }

  @Test
  @DisplayName("Should convert to Map")
  void shouldConvertToMap() {
    MetadataVO metadata = MetadataVO.empty()
      .with("key1", "value1")
      .withInt("key2", 123);

    Map<String, Object> map = metadata.toMap();

    assertEquals("value1", map.get("key1"));
    assertEquals(123, map.get("key2"));
  }

  @Test
  @DisplayName("Should convert to JSON string")
  void shouldConvertToJsonString() {
    MetadataVO metadata = MetadataVO.empty()
      .with("name", "test");

    String json = metadata.toJson();

    assertNotNull(json);
    assertTrue(json.contains("name"));
    assertTrue(json.contains("test"));
  }

  @Test
  @DisplayName("Should handle common metadata patterns - tags")
  void shouldHandleTagsPattern() {
    MetadataVO metadata = MetadataVO.empty()
      .withTags("tag1", "tag2", "tag3");

    List<String> tags = metadata.getTags();
    assertEquals(3, tags.size());
    assertTrue(tags.contains("tag1"));
  }

  @Test
  @DisplayName("Should handle common metadata patterns - description")
  void shouldHandleDescriptionPattern() {
    MetadataVO metadata = MetadataVO.empty()
      .withDescription("Test description");

    assertEquals("Test description", metadata.getDescription());
  }

  @Test
  @DisplayName("Should handle common metadata patterns - source")
  void shouldHandleSourcePattern() {
    MetadataVO metadata = MetadataVO.empty()
      .withSource("test-source");

    assertEquals("test-source", metadata.getSource());
  }

  @Test
  @DisplayName("Should handle common metadata patterns - version")
  void shouldHandleVersionPattern() {
    MetadataVO metadata = MetadataVO.empty()
      .withVersion("1.0.0");

    assertEquals("1.0.0", metadata.getVersion());
  }

  @Test
  @DisplayName("Should return default values when key not found")
  void shouldReturnDefaultValues() {
    MetadataVO metadata = MetadataVO.empty();

    assertEquals("default", metadata.getString("nonexistent", "default"));
    assertEquals(42, metadata.getInt("nonexistent", 42));
    assertTrue(metadata.getBoolean("nonexistent", true));
  }

  @Test
  @DisplayName("Should be immutable - modifications create new instances")
  void shouldBeImmutable() {
    MetadataVO original = MetadataVO.empty()
      .with("key1", "value1");

    MetadataVO modified = original.with("key2", "value2");

    assertTrue(original.has("key1"));
    assertFalse(original.has("key2"));
    assertTrue(modified.has("key1"));
    assertTrue(modified.has("key2"));
  }

  @Test
  @DisplayName("Should handle null values gracefully")
  void shouldHandleNullValues() {
    MetadataVO metadata = MetadataVO.empty()
      .with("key1", null);

    assertFalse(metadata.has("key1"));
  }

  @Test
  @DisplayName("Should throw exception for null key")
  void shouldThrowExceptionForNullKey() {
    MetadataVO metadata = MetadataVO.empty();

    assertThrows(IllegalArgumentException.class, () -> {
      metadata.with(null, "value");
    });
  }

  @Test
  @DisplayName("Should handle empty string list")
  void shouldHandleEmptyStringList() {
    MetadataVO metadata = MetadataVO.empty()
      .withStringList("emptyList", List.of());

    assertFalse(metadata.has("emptyList"));
  }

  @Test
  @DisplayName("Should create metadata from JsonNode")
  void shouldCreateMetadataFromJsonNode() throws Exception {
    JsonNode node = objectMapper.readTree("{\"key\":\"value\"}");

    MetadataVO metadata = MetadataVO.of(node);

    assertEquals("value", metadata.getString("key"));
  }
}
