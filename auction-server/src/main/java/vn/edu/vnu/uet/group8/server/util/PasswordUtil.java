package vn.edu.vnu.uet.group8.server.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Lớp tiện ích xử lý bảo mật mật khẩu bằng thuật toán BCrypt.
 *
 * <p>Sử dụng cơ chế băm một chiều (One-way Hashing) kết hợp với Salt ngẫu nhiên để chống lại các
 * cuộc tấn công Rainbow Table và Brute-force.
 *
 * <p>Cấu trúc chuỗi băm của BCrypt: {@code $2a$[cost]$[22-char-salt][31-char-hash]}
 *
 * @version 1.0
 */
public class PasswordUtil {
  /**
   * Tham số độ phức tạp (Logarithmic Cost Factor).
   *
   * <p>Giá trị 12 nghĩa là thuật toán sẽ thực hiện 2^12 (4096) vòng lặp băm. Đây là mức cân bằng
   * tối ưu giữa trải nghiệm người dùng (~250ms) và khả năng kháng lại các dàn máy dùng để phá mật
   * khẩu.
   */
  private static final int COST = 12;

  /**
   * Băm mật khẩu từ văn bản thuần sang chuỗi bảo mật.
   *
   * <p>Hàm này tự động tạo một Salt ngẫu nhiên và nhúng trực tiếp vào chuỗi kết quả.
   *
   * @param plainPassword Mật khẩu người dùng nhập từ giao diện (Plain Text).
   * @return Chuỗi Hash dài 60 ký tự để lưu vào cột password_hash trong Database.
   */
  public static String hash(String plainPassword) {
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST));
  }

  /**
   * Xác thực mật khẩu khi người dùng đăng nhập.
   *
   * <p>Thuật toán sẽ tự trích xuất Salt từ {@code storedHash} để băm thử {@code plainPassword} và
   * so sánh kết quả.
   *
   * @param plainPassword Mật khẩu do người dùng vừa nhập vào.
   * @param storedHash Chuỗi băm đã lưu trong Database từ trước.
   * @return {@code true} nếu mật khẩu khớp, {@code false} nếu sai mật khẩu.
   */
  public static boolean verify(String plainPassword, String storedHash) {
    return BCrypt.checkpw(plainPassword, storedHash);
  }
}