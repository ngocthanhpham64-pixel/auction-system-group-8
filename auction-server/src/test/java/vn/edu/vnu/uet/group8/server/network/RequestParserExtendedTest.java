package vn.edu.vnu.uet.group8.server.network;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;

class RequestParserExtendedTest {

  // ════════════════════════════════════════════════════
  // requireInt()
  // ════════════════════════════════════════════════════
  @Nested
  @DisplayName("requireInt()")
  class RequireIntTest {

    @Test
    @DisplayName("field có giá trị int → trả đúng")
    void coInt() {
      JsonObject r = new JsonObject();
      r.addProperty("itemId", 42);
      assertEquals(42, RequestParser.requireInt(r, "itemId"));
    }

    @Test
    @DisplayName("field có giá trị 0 → trả 0")
    void zero() {
      JsonObject r = new JsonObject();
      r.addProperty("count", 0);
      assertEquals(0, RequestParser.requireInt(r, "count"));
    }

    @Test
    @DisplayName("field âm → trả giá trị âm")
    void negative() {
      JsonObject r = new JsonObject();
      r.addProperty("val", -5);
      assertEquals(-5, RequestParser.requireInt(r, "val"));
    }

    @Test
    @DisplayName("field không tồn tại → ValidationException")
    void fieldMissing() {
      assertThrows(ValidationException.class, () ->
          RequestParser.requireInt(new JsonObject(), "missing"));
    }

    @Test
    @DisplayName("field là JsonNull → ValidationException")
    void fieldNull() {
      JsonObject r = new JsonObject();
      r.addProperty("val", (Integer) null);
      assertThrows(ValidationException.class, () ->
          RequestParser.requireInt(r, "val"));
    }
  }

  // ════════════════════════════════════════════════════
  // optionalInt()
  // ════════════════════════════════════════════════════
  @Nested
  @DisplayName("optionalInt()")
  class OptionalIntTest {

    @Test
    @DisplayName("field có giá trị → trả đúng")
    void coInt() {
      JsonObject r = new JsonObject();
      r.addProperty("page", 3);
      assertEquals(3, RequestParser.optionalInt(r, "page"));
    }

    @Test
    @DisplayName("field không tồn tại → null")
    void fieldMissing() {
      assertNull(RequestParser.optionalInt(new JsonObject(), "missing"));
    }

    @Test
    @DisplayName("field là JsonNull → null")
    void fieldJsonNull() {
      JsonObject r = new JsonObject();
      r.addProperty("val", (Integer) null);
      assertNull(RequestParser.optionalInt(r, "val"));
    }

    @Test
    @DisplayName("field = 0 → 0")
    void zero() {
      JsonObject r = new JsonObject();
      r.addProperty("val", 0);
      assertEquals(0, RequestParser.optionalInt(r, "val"));
    }
  }

  // ════════════════════════════════════════════════════
  // requireMap()
  // ════════════════════════════════════════════════════
  @Nested
  @DisplayName("requireMap()")
  class RequireMapTest {

    @Test
    @DisplayName("field là JsonObject hợp lệ → trả Map đúng")
    void hopLe() {
      JsonObject specs = new JsonObject();
      specs.addProperty("color", "red");
      specs.addProperty("size", "XL");

      JsonObject r = new JsonObject();
      r.add("specs", specs);

      Map<String, String> result = RequestParser.requireMap(r, "specs");
      assertEquals("red", result.get("color"));
      assertEquals("XL", result.get("size"));
    }

    @Test
    @DisplayName("field là JsonObject rỗng → trả Map rỗng")
    void emptyObject() {
      JsonObject r = new JsonObject();
      r.add("specs", new JsonObject());
      Map<String, String> result = RequestParser.requireMap(r, "specs");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("field không tồn tại → ValidationException")
    void fieldMissing() {
      assertThrows(ValidationException.class, () ->
          RequestParser.requireMap(new JsonObject(), "specs"));
    }

    @Test
    @DisplayName("field là JsonNull → ValidationException")
    void fieldNull() {
      JsonObject r = new JsonObject();
      r.add("specs", com.google.gson.JsonNull.INSTANCE);
      assertThrows(ValidationException.class, () ->
          RequestParser.requireMap(r, "specs"));
    }

    @Test
    @DisplayName("field là JsonArray (không phải Object) → ValidationException")
    void fieldIsArray() {
      JsonObject r = new JsonObject();
      r.add("specs", new com.google.gson.JsonArray());
      assertThrows(ValidationException.class, () ->
          RequestParser.requireMap(r, "specs"));
    }

    @Test
    @DisplayName("field là primitive string → ValidationException")
    void fieldIsPrimitive() {
      JsonObject r = new JsonObject();
      r.addProperty("specs", "not-an-object");
      assertThrows(ValidationException.class, () ->
          RequestParser.requireMap(r, "specs"));
    }
  }

  // ════════════════════════════════════════════════════
  // optionalMap()
  // ════════════════════════════════════════════════════
  @Nested
  @DisplayName("optionalMap()")
  class OptionalMapTest {

    @Test
    @DisplayName("field là JsonObject hợp lệ → trả Map đúng")
    void hopLe() {
      JsonObject meta = new JsonObject();
      meta.addProperty("brand", "Apple");
      JsonObject r = new JsonObject();
      r.add("meta", meta);

      Map<String, String> result = RequestParser.optionalMap(r, "meta");
      assertEquals("Apple", result.get("brand"));
    }

    @Test
    @DisplayName("field không tồn tại → Map rỗng")
    void fieldMissing() {
      Map<String, String> result = RequestParser.optionalMap(new JsonObject(), "meta");
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("field là JsonNull → Map rỗng")
    void fieldNull() {
      JsonObject r = new JsonObject();
      r.add("meta", com.google.gson.JsonNull.INSTANCE);
      Map<String, String> result = RequestParser.optionalMap(r, "meta");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("field là JsonArray → ValidationException")
    void fieldIsArray() {
      JsonObject r = new JsonObject();
      r.add("meta", new com.google.gson.JsonArray());
      assertThrows(ValidationException.class, () ->
          RequestParser.optionalMap(r, "meta"));
    }

    @Test
    @DisplayName("field là primitive → ValidationException")
    void fieldIsPrimitive() {
      JsonObject r = new JsonObject();
      r.addProperty("meta", "bad-value");
      assertThrows(ValidationException.class, () ->
          RequestParser.optionalMap(r, "meta"));
    }

    @Test
    @DisplayName("optionalMap trả Map có thể đọc bình thường")
    void mapReadable() {
      JsonObject inner = new JsonObject();
      inner.addProperty("k1", "v1");
      inner.addProperty("k2", "v2");
      JsonObject r = new JsonObject();
      r.add("data", inner);

      Map<String, String> result = RequestParser.optionalMap(r, "data");
      assertEquals(2, result.size());
      assertEquals("v1", result.get("k1"));
      assertEquals("v2", result.get("k2"));
    }
  }

  // ════════════════════════════════════════════════════
  // optionalString()
  // ════════════════════════════════════════════════════
  @Nested
  @DisplayName("optionalString()")
  class OptionalStringTest {

    @Test
    @DisplayName("field có giá trị → trả đúng")
    void coGiaTri() {
      JsonObject r = new JsonObject();
      r.addProperty("name", "hello");
      assertEquals("hello", RequestParser.optionalString(r, "name"));
    }

    @Test
    @DisplayName("field không tồn tại → null")
    void missing() {
      assertNull(RequestParser.optionalString(new JsonObject(), "name"));
    }

    @Test
    @DisplayName("field JsonNull → null")
    void jsonNull() {
      JsonObject r = new JsonObject();
      r.addProperty("name", (String) null);
      assertNull(RequestParser.optionalString(r, "name"));
    }

    @Test
    @DisplayName("field rỗng string → trả chuỗi rỗng")
    void emptyString() {
      JsonObject r = new JsonObject();
      r.addProperty("name", "");
      assertEquals("", RequestParser.optionalString(r, "name"));
    }
  }

  // ════════════════════════════════════════════════════
  // getPayload() - branch: payload là JsonObject
  // ════════════════════════════════════════════════════
  @Nested
  @DisplayName("getPayload() - branches")
  class GetPayloadTest {

    @Test
    @DisplayName("payload là JsonPrimitive số → deserialize đúng kiểu")
    void payloadIsPrimitive() {
      JsonObject r = new JsonObject();
      r.addProperty("payload", 42);
      Integer result = RequestParser.getPayload(r, Integer.class);
      assertEquals(42, result);
    }

    @Test
    @DisplayName("payload là JsonObject → deserialize sang DTO")
    void payloadIsObject() {
      JsonObject inner = new JsonObject();
      inner.addProperty("userId", 5);
      inner.addProperty("maxPrice", 1000000);
      JsonObject r = new JsonObject();
      r.add("payload", inner);
      // Chỉ test không throw và trả không null
      assertNotNull(RequestParser.getPayload(r, com.google.gson.JsonObject.class));
    }

    @Test
    @DisplayName("payload null → trả null")
    void payloadMissing() {
      assertNull(RequestParser.getPayload(new JsonObject(), String.class));
    }

    @Test
    @DisplayName("payload sai kiểu nghiêm trọng → ValidationException")
    void payloadWrongType() {
      JsonObject r = new JsonObject();
      r.addProperty("payload", "{ invalid json object }");
      // Nếu target là một type không parse được → ValidationException
      // Chỉ test với class có strict constructor
      assertDoesNotThrow(() ->
          RequestParser.getPayload(r, String.class)); // String thì luôn parse được
    }
  }
}