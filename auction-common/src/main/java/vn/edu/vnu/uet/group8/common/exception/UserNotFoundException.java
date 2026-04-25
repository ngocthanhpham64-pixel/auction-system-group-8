package vn.edu.vnu.uet.group8.common.exception;

/**
 * Ngoại lệ được ném ra khi không tìm thấy thông tin người dùng trong hệ thống.
 */
public class UserNotFoundException extends RuntimeException {
  /**
   * Khởi tạo ngoại lệ với thông báo chi tiết.
   * @param message Nội dung thông báo lỗi (ví dụ: "Không tìm thấy username: thanh_uet")
   */
  public UserNotFoundException(String message) {
    super(message);
  }

  /**
  * Khởi tạo ngoại lệ với thông báo và nguyên nhân gốc rễ (Cause).
  * Dùng khi bạn muốn bọc một lỗi khác (như SQLException) vào lỗi này.
  */
  public UserNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public UserNotFoundException(int userId) {
      super("Không tìm thấy người dùng với ID: " + userId);
  }
}

