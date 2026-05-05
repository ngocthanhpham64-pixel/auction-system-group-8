package vn.edu.vnu.uet.group8.common.dto.response;

import java.util.Objects;

/**
 * Generic wrapper cho mọi response từ Server → Client qua Socket.
 *
 * Nguyên tắc thiết kế:
 *   - KHÔNG implements Serializable — dùng GSON
 *   - Generic T để tái sử dụng cho mọi loại data
 *   - Constructor private — chỉ tạo qua static factory
 *   - Immutable sau khi tạo — không có setter
 *
 * Cách dùng:
 *   ResponseDTO<ItemDTO>        → trả 1 item
 *   ResponseDTO<List<ItemDTO>>  → trả danh sách
 *   ResponseDTO<UserSummaryDTO> → trả thông tin user sau login
 *   ResponseDTO<Void>           → chỉ báo thành công/thất bại
 *
 * @param <T> Kiểu dữ liệu trả về — null nếu không có data
 */
public class ResponseDTO<T> {

  // ── Bắt buộc có trong mọi response ──────────────────
  private boolean success;
  private String  message;

  // ── Dữ liệu kèm theo — null nếu thất bại ────────────
  private T data;

  // ── Metadata phụ trợ ─────────────────────────────────
  private String  requestType;  // loại request tương ứng — Client
                                // dùng để route đúng handler
  private long    timestamp;    // epoch millis — debug và log

  // Constructor private — không ai new trực tiếp được
  private ResponseDTO() {
    this.timestamp = System.currentTimeMillis();
  }

  // ════════════════════════════════════════════════════
  // STATIC FACTORY METHODS
  // ════════════════════════════════════════════════════

  /**
   * Thành công, có data kèm theo.
   * Dùng cho: getItem, login, getItemList,...
   *
   * ResponseDTO.ok(itemDTO)
   * ResponseDTO.ok(itemDTOList)
   */
  public static <T> ResponseDTO<T> ok(T data) {
    ResponseDTO<T> r = new ResponseDTO<>();
    r.success = true;
    r.message = "OK";
    r.data    = data;
    return r;
  }

  /**
   * Thành công, có data và message tùy chỉnh.
   * Dùng cho: placeBid thành công muốn thông báo rõ hơn.
   *
   * ResponseDTO.ok(bidResponse, "Đặt giá thành công!")
   */
  public static <T> ResponseDTO<T> ok(T data, String message) {
    ResponseDTO<T> r = new ResponseDTO<>();
    r.success = true;
    r.message = Objects.requireNonNullElse(message, "OK");
    r.data    = data;
    return r;
  }

  /**
   * Thành công, không có data.
   * Dùng cho: softDelete, updateProfile, addRole,...
   * Những action chỉ cần biết thành công hay không.
   *
   * ResponseDTO.ok()
   */
  public static ResponseDTO<Void> ok() {
    return ok(null, "Thành công");
  }

  /**
   * Thất bại với lý do cụ thể.
   * Dùng cho: item không tồn tại, giá không hợp lệ,
   *           tài khoản bị khoá, không đủ quyền,...
   *
   * ResponseDTO.fail("Item không tồn tại")
   * ResponseDTO.fail("Giá đặt phải cao hơn giá hiện tại")
   */
  public static <T> ResponseDTO<T> fail(String message) {
    ResponseDTO<T> r = new ResponseDTO<>();
    r.success = false;
    r.message = Objects.requireNonNullElse(message,
        "Đã xảy ra lỗi không xác định");
    r.data    = null;
    return r;
  }

  /**
   * Lỗi hệ thống — dùng khi catch Exception bất ngờ.
   * Không lộ stack trace hay thông tin nội bộ ra Client.
   *
   * ResponseDTO.error()
   */
  public static <T> ResponseDTO<T> error() {
    return fail("Lỗi hệ thống, vui lòng thử lại sau");
  }

  // ════════════════════════════════════════════════════
  // BUILDER CHAIN — gắn thêm metadata sau khi tạo
  // ════════════════════════════════════════════════════

  /**
   * Gắn loại request để Client biết route về handler nào.
   * Server gọi sau ok() hoặc fail() nếu cần.
   *
   * ResponseDTO.ok(itemDTO).withType("GET_ITEM")
   */
  public ResponseDTO<T> withType(String requestType) {
    this.requestType = requestType;
    return this;
  }

  // ════════════════════════════════════════════════════
  // GETTERS
  // ════════════════════════════════════════════════════

  public boolean isSuccess()      { return success; }
  public String  getMessage()     { return message; }
  public T       getData()        { return data; }
  public String  getRequestType() { return requestType; }
  public long    getTimestamp()   { return timestamp; }

  /**
   * Kiểm tra có data không — tránh NullPointerException ở Client.
   * Luôn gọi hasData() trước getData() nếu không chắc.
   */
  public boolean hasData() {
    return data != null;
  }

  @Override
  public String toString() {
    return "ResponseDTO{" +
            "success="     + success     +
            ", message='"  + message     + '\'' +
            ", type='"     + requestType + '\'' +
            ", hasData="   + hasData()   +
            ", timestamp=" + timestamp   +
            '}';
  }
}