package vn.edu.vnu.uet.group8.server.network;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.uet.group8.common.dto.request.BidRequest;
import vn.edu.vnu.uet.group8.common.dto.request.LoginRequest;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;

/**
 * Test cho {@link RequestParser} - static helper, không cần mock.
 */
class RequestParserTest {

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getAction()")
  class GetActionTest {

    @Test
    @DisplayName("Có action → trả giá trị")
    void coAction() {
      JsonObject req = new JsonObject();
      req.addProperty("action", "LOGIN");
      assertEquals("LOGIN", RequestParser.getAction(req));
    }

    @Test
    @DisplayName("Thiếu action → ném ValidationException")
    void thieuAction() {
      JsonObject req = new JsonObject();
      assertThrows(ValidationException.class, () -> RequestParser.getAction(req));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getRequestId()")
  class GetRequestIdTest {

    @Test
    @DisplayName("Có requestId → trả giá trị")
    void coRequestId() {
      JsonObject req = new JsonObject();
      req.addProperty("requestId", "req-1");
      assertEquals("req-1", RequestParser.getRequestId(req));
    }

    @Test
    @DisplayName("Thiếu requestId → trả null (không ném)")
    void thieuRequestId() {
      JsonObject req = new JsonObject();
      assertNull(RequestParser.getRequestId(req));
    }

    @Test
    @DisplayName("requestId là JsonNull → trả null")
    void requestIdJsonNull() {
      JsonObject req = new JsonObject();
      req.add("requestId", com.google.gson.JsonNull.INSTANCE);
      assertNull(RequestParser.getRequestId(req));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getToken()")
  class GetTokenTest {

    @Test
    @DisplayName("Có token → trả giá trị")
    void coToken() {
      JsonObject req = new JsonObject();
      req.addProperty("token", "tok-xyz");
      assertEquals("tok-xyz", RequestParser.getToken(req));
    }

    @Test
    @DisplayName("Thiếu token → trả null")
    void thieuToken() {
      JsonObject req = new JsonObject();
      assertNull(RequestParser.getToken(req));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("getPayload()")
  class GetPayloadTest {

    @Test
    @DisplayName("Có payload hợp lệ → deserialize đúng DTO")
    void payloadHopLe() {
      JsonObject payload = new JsonObject();
      payload.addProperty("email", "a@e.com");
      payload.addProperty("password", "pass");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      LoginRequest result = RequestParser.getPayload(req, LoginRequest.class);
      assertNotNull(result);
      assertEquals("a@e.com", result.getEmail());
      assertEquals("pass", result.getPassword());
    }

    @Test
    @DisplayName("Thiếu payload → trả null (không ném)")
    void thieuPayload() {
      JsonObject req = new JsonObject();
      assertNull(RequestParser.getPayload(req, LoginRequest.class));
    }

    @Test
    @DisplayName("payload JsonNull → trả null")
    void payloadJsonNull() {
      JsonObject req = new JsonObject();
      req.add("payload", com.google.gson.JsonNull.INSTANCE);
      assertNull(RequestParser.getPayload(req, LoginRequest.class));
    }

    @Test
    void payloadSaiDinhDang() {
      JsonObject req = new JsonObject();
      req.addProperty("payload", "abc");

      assertThrows(
          ValidationException.class,
          () -> RequestParser.getPayload(req, BidRequest.class)
      );
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("requireString()")
  class RequireStringTest {

    @Test
    @DisplayName("Có field → trả giá trị")
    void coField() {
      JsonObject req = new JsonObject();
      req.addProperty("name", "Alice");
      assertEquals("Alice", RequestParser.requireString(req, "name"));
    }

    @Test
    @DisplayName("Thiếu field → ném ValidationException")
    void thieuField() {
      JsonObject req = new JsonObject();
      ValidationException ex = assertThrows(ValidationException.class,
          () -> RequestParser.requireString(req, "name"));
      assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    @DisplayName("Field JsonNull → ném ValidationException")
    void fieldJsonNull() {
      JsonObject req = new JsonObject();
      req.add("name", com.google.gson.JsonNull.INSTANCE);
      assertThrows(ValidationException.class,
          () -> RequestParser.requireString(req, "name"));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("requireInt()")
  class RequireIntTest {

    @Test
    @DisplayName("Có field int → trả giá trị")
    void coField() {
      JsonObject req = new JsonObject();
      req.addProperty("age", 25);
      assertEquals(25, RequestParser.requireInt(req, "age"));
    }

    @Test
    @DisplayName("Thiếu field → ném ValidationException")
    void thieuField() {
      JsonObject req = new JsonObject();
      assertThrows(ValidationException.class,
          () -> RequestParser.requireInt(req, "age"));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("optionalInt()")
  class OptionalIntTest {

    @Test
    @DisplayName("Có field → trả Integer")
    void coField() {
      JsonObject req = new JsonObject();
      req.addProperty("age", 30);
      assertEquals(30, RequestParser.optionalInt(req, "age"));
    }

    @Test
    @DisplayName("Thiếu field → trả null")
    void thieuField() {
      JsonObject req = new JsonObject();
      assertNull(RequestParser.optionalInt(req, "age"));
    }

    @Test
    @DisplayName("Field JsonNull → trả null")
    void jsonNull() {
      JsonObject req = new JsonObject();
      req.add("age", com.google.gson.JsonNull.INSTANCE);
      assertNull(RequestParser.optionalInt(req, "age"));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("requireMap()")
  class RequireMapTest {

    @Test
    @DisplayName("Có map hợp lệ → trả Map<String,String>")
    void mapHopLe() {
      JsonObject mapObj = new JsonObject();
      mapObj.addProperty("key1", "val1");
      mapObj.addProperty("key2", "val2");
      JsonObject req = new JsonObject();
      req.add("specs", mapObj);

      Map<String, String> result = RequestParser.requireMap(req, "specs");
      assertEquals(2, result.size());
      assertEquals("val1", result.get("key1"));
      assertEquals("val2", result.get("key2"));
    }

    @Test
    @DisplayName("Thiếu field → ném ValidationException")
    void thieuField() {
      JsonObject req = new JsonObject();
      assertThrows(ValidationException.class,
          () -> RequestParser.requireMap(req, "specs"));
    }

    @Test
    @DisplayName("Field không phải JsonObject (là JsonArray) → ném ValidationException")
    void khongPhaiObject() {
      JsonObject req = new JsonObject();
      req.add("specs", new JsonArray());
      assertThrows(ValidationException.class,
          () -> RequestParser.requireMap(req, "specs"));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("optionalMap()")
  class OptionalMapTest {

    @Test
    @DisplayName("Thiếu field → trả emptyMap (không ném)")
    void thieuField() {
      JsonObject req = new JsonObject();
      Map<String, String> result = RequestParser.optionalMap(req, "specs");
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Có map hợp lệ → trả Map")
    void mapHopLe() {
      JsonObject mapObj = new JsonObject();
      mapObj.addProperty("k", "v");
      JsonObject req = new JsonObject();
      req.add("specs", mapObj);

      Map<String, String> result = RequestParser.optionalMap(req, "specs");
      assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Field là JsonArray → ném")
    void khongPhaiObject() {
      JsonObject req = new JsonObject();
      req.add("specs", new JsonArray());
      assertThrows(ValidationException.class,
          () -> RequestParser.optionalMap(req, "specs"));
    }
  }
}