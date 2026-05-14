package vn.edu.vnu.uet.group8.server.network;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * Helper static để extract field/payload từ JsonObject request.
 *
 * <p>ClientHandler chỉ là "người đưa thư" — không tự parse từng field.
 * Helper này cho phép ClientHandler lấy đúng kiểu dữ liệu trước khi
 * giao cho controller.
 */
public final class RequestParser {
  private static final Gson GSON = GsonUtil.GSON;

  private RequestParser() {}

  /**
   * Lấy field "action" — luôn bắt buộc.
   * @throws ValidationException nếu thiếu hoặc null
   */
  public static String getAction(JsonObject request) {
    return requireString(request, "action");
  }

  /** Lấy "requestId", có thể null. */
  public static String getRequestId(JsonObject request) {
    JsonElement el = request.get("requestId");
    return (el == null || el.isJsonNull()) ? null : el.getAsString();
  }

  /** Lấy "token", có thể null (cho LOGIN/REGISTER). */
  public static String getToken(JsonObject request) {
    JsonElement el = request.get("token");
    return (el == null || el.isJsonNull()) ? null : el.getAsString();
  }

  /**
   * Deserialize phần "payload" của request thành DTO cụ thể.
   * Trả null nếu payload không có (vd: LOGOUT, HEARTBEAT).
   *
   * @param request JsonObject toàn bộ request
   * @param payloadType class của DTO mong muốn (vd: LoginRequest.class)
   */
  public static <T> T getPayload(JsonObject request, Class<T> payloadType) {
    JsonElement payload = request.get("payload");
    if (payload == null || payload.isJsonNull()) return null;
    try {
      return GSON.fromJson(payload, payloadType);
    } catch (Exception e) {
      throw new ValidationException(
          "Payload không đúng định dạng " + payloadType.getSimpleName()
          + ": " + e.getMessage());
    }
  }

  // ---- helpers cho field nguyên thuỷ ----

  public static String requireString(JsonObject request, String field) {
    JsonElement el = request.get(field);
    if (el == null || el.isJsonNull()) {
      throw new ValidationException("Thiếu field bắt buộc: " + field);
    }
    return el.getAsString();
  }

  public static int requireInt(JsonObject request, String field) {
    JsonElement el = request.get(field);
    if (el == null || el.isJsonNull()) {
      throw new ValidationException("Thiếu field bắt buộc: " + field);
    }
    return el.getAsInt();
  }

  public static Integer optionalInt(JsonObject request, String field) {
    JsonElement el = request.get(field);
    return (el == null || el.isJsonNull()) ? null : el.getAsInt();
  }

  /**
   * Lấy ra Map<String, String> bắt buộc. Ném lỗi nếu thiếu hoặc sai định dạng.
   */
  public static Map<String, String> requireMap(JsonObject request, String field) {
    JsonElement el = request.get(field);
    if (el == null || el.isJsonNull()) {
      throw new ValidationException("Thiếu field bắt buộc: " + field);
    }
    if (!el.isJsonObject()) {
      throw new ValidationException("Field " + field + " phải là một JSON Object");
    }

    Type type = new TypeToken<Map<String, String>>(){}.getType();
    try {
      return GSON.fromJson(el, type);
    } catch (Exception e) {
      throw new ValidationException("Định dạng " + field + " không hợp lệ: " + e.getMessage());
    }
  }

  /**
   * Lấy ra Map<String, String> tuỳ chọn. Trả về Map rỗng nếu không có.
   */
  public static Map<String, String> optionalMap(JsonObject request, String field) {
    JsonElement el = request.get(field);
    if (el == null || el.isJsonNull()) {
      return Collections.emptyMap();
    }
    if (!el.isJsonObject()) {
      throw new ValidationException("Field " + field + " phải là một JSON Object");
    }

    Type type = new TypeToken<Map<String, String>>(){}.getType();
    try {
      return GSON.fromJson(el, type);
    } catch (Exception e) {
      throw new ValidationException("Định dạng " + field + " không hợp lệ: " + e.getMessage());
    }
  }
}
