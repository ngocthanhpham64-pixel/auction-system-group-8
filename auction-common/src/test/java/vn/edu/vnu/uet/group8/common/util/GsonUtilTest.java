package vn.edu.vnu.uet.group8.common.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/** Test cho {@link GsonUtil} - test custom Instant adapter và các tiện ích chuyển đổi. */
class GsonUtilTest {

  @Nested
  @DisplayName("GSON instance")
  class InstanceTest {

    @Test
    @DisplayName("GSON là singleton (static final)")
    void singleton() {
      assertSame(GsonUtil.GSON, GsonUtil.GSON);
    }

    @Test
    @DisplayName("GSON không null")
    void notNull() {
      assertNotNull(GsonUtil.GSON);
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Instant serialization/deserialization")
  class InstantTest {

    @Test
    @DisplayName("Serialize Instant → string ISO-8601")
    void serialize() {
      Instant instant = Instant.parse("2026-01-15T10:30:00Z");
      String json = GsonUtil.GSON.toJson(instant);
      assertTrue(json.contains("2026-01-15T10:30:00Z"), "JSON phải chứa ISO-8601 string");
    }

    @Test
    @DisplayName("Deserialize string ISO-8601 → Instant")
    void deserialize() {
      String json = "\"2026-01-15T10:30:00Z\"";
      Instant parsed = GsonUtil.GSON.fromJson(json, Instant.class);
      assertEquals(Instant.parse("2026-01-15T10:30:00Z"), parsed);
    }

    @Test
    @DisplayName("Round-trip Instant serialize → deserialize")
    void roundTrip() {
      Instant original = Instant.parse("2026-05-23T15:00:00Z");
      String json = GsonUtil.GSON.toJson(original);
      Instant parsed = GsonUtil.GSON.fromJson(json, Instant.class);
      assertEquals(original, parsed);
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("Standard types")
  class StandardTypeTest {

    @Test
    @DisplayName("Serialize/deserialize String")
    void stringRoundtrip() {
      String json = GsonUtil.GSON.toJson("Hello world");
      assertEquals("Hello world", GsonUtil.GSON.fromJson(json, String.class));
    }

    @Test
    @DisplayName("Serialize/deserialize Integer")
    void intRoundtrip() {
      String json = GsonUtil.GSON.toJson(42);
      assertEquals(42, GsonUtil.GSON.fromJson(json, Integer.class));
    }

    @Test
    @DisplayName("Serialize/deserialize BigDecimal")
    void bigDecimalRoundtrip() {
      BigDecimal original = new BigDecimal("12345.6789");
      String json = GsonUtil.GSON.toJson(original);
      BigDecimal parsed = GsonUtil.GSON.fromJson(json, BigDecimal.class);
      assertEquals(0, original.compareTo(parsed));
    }

    @Test
    @DisplayName("Serialize JsonObject")
    void jsonObject() {
      JsonObject obj = new JsonObject();
      obj.addProperty("name", "Alice");
      obj.addProperty("age", 30);

      String json = GsonUtil.GSON.toJson(obj);
      assertTrue(json.contains("Alice"));
      assertTrue(json.contains("30"));
    }

    @Test
    @DisplayName("Deserialize null string trả null")
    void deserializeNull() {
      assertNull(GsonUtil.GSON.fromJson("null", String.class));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("DTO serialization (object có nested Instant)")
  class DtoTest {

    static class Sample {
      String name;
      Instant createdAt;
      BigDecimal price;

      Sample(String name, Instant createdAt, BigDecimal price) {
        this.name = name;
        this.createdAt = createdAt;
        this.price = price;
      }
    }

    @Test
    @DisplayName("Round-trip DTO có Instant + BigDecimal")
    void roundtripDto() {
      Sample original =
          new Sample("iPhone", Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("100"));
      String json = GsonUtil.GSON.toJson(original);
      Sample parsed = GsonUtil.GSON.fromJson(json, Sample.class);

      assertEquals(original.name, parsed.name);
      assertEquals(original.createdAt, parsed.createdAt);
      assertEquals(0, original.price.compareTo(parsed.price));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("GsonUtil.toObject")
  class ToObjectTest {

    @Test
    @DisplayName("toObject với data null -> trả về null")
    void toObjectNull() {
      assertNull(GsonUtil.toObject(null, String.class));
    }

    @Test
    @DisplayName("toObject với data đã đúng kiểu -> cast và trả về trực tiếp")
    void toObjectCorrectType() {
      String value = "hello";
      assertSame(value, GsonUtil.toObject(value, String.class));
    }

    @Test
    @DisplayName("toObject với Map -> convert thành DTO đúng")
    void toObjectConversion() {
      Map<String, Object> map = Map.of("name", "Laptop", "price", new BigDecimal("999.99"));
      DtoTest.Sample sample = GsonUtil.toObject(map, DtoTest.Sample.class);
      assertNotNull(sample);
      assertEquals("Laptop", sample.name);
      assertEquals(0, new BigDecimal("999.99").compareTo(sample.price));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("GsonUtil.toList")
  class ToListTest {

    @Test
    @DisplayName("toList với data null -> trả về danh sách rỗng")
    void toListNull() {
      List<String> result = GsonUtil.toList(null, String.class);
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toList với danh sách Map -> convert thành danh sách DTO đúng")
    void toListConversion() {
      List<Map<String, Object>> mapList =
          List.of(
              Map.of("name", "A", "price", new BigDecimal("10")),
              Map.of("name", "B", "price", new BigDecimal("20")));
      List<DtoTest.Sample> list = GsonUtil.toList(mapList, DtoTest.Sample.class);
      assertEquals(2, list.size());
      assertEquals("A", list.get(0).name);
      assertEquals("B", list.get(1).name);
    }
  }
}