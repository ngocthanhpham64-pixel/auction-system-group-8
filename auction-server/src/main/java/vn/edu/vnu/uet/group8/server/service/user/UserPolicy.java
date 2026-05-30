package vn.edu.vnu.uet.group8.server.service.user;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import vn.edu.vnu.uet.group8.common.exception.UserServiceException;

/**
 * Tập trung toàn bộ hằng số và quy tắc nghiệp vụ của User.
 *
 * Lý do tách riêng:
 *   - Thay đổi policy → sửa một chỗ duy nhất
 *   - Không phải recompile toàn bộ Service
 *   - Dễ đọc, dễ review khi product owner muốn kiểm tra rule
 *
 * Sau này có thể load từ config.properties hoặc DB
 * mà không cần động đến UserService.
 */
public final class UserPolicy {

  // Không cho khởi tạo — đây là utility class
  private UserPolicy() {}

  // ── Username ──────────────────────────────────────────
  public static final int    USERNAME_MIN_LENGTH  = 3;
  public static final int    USERNAME_MAX_LENGTH  = 50;
  // Chỉ cho phép chữ, số, dấu _ và dấu .
  public static final Pattern USERNAME_PATTERN    =
    Pattern.compile("^[a-zA-Z0-9_.]+$");

  // ── Password ──────────────────────────────────────────
  public static final int    PASSWORD_MIN_LENGTH  = 8;
  public static final int    PASSWORD_MAX_LENGTH  = 100;
  // Phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số
  public static final Pattern PASSWORD_PATTERN    =
    Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");

  // ── Email ────────────────────────────────────────────
  public static final Pattern EMAIL_PATTERN       =
    Pattern.compile(
        "^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

  // ── Balance ───────────────────────────────────────────
  // Số tiền nạp tối thiểu mỗi lần
  public static final BigDecimal TOPUP_MIN_AMOUNT =
    new BigDecimal("10000");

  // Số tiền nạp tối đa mỗi lần
  public static final BigDecimal TOPUP_MAX_AMOUNT =
    new BigDecimal("1000000000");

  // Số dư tối đa trong ví — tránh overflow
  public static final BigDecimal BALANCE_MAX_TOTAL =
    new BigDecimal("10000000000");

  // ── Validation helpers ────────────────────────────────

  /**
   * Validate username — trả null nếu hợp lệ,
   * trả message lỗi nếu không.
   */
  public static String validateUsername(String username) {
    checkRequireString(username, "Username không được để trống");

    String trimmed = username.trim();

    checkRequireLength(trimmed, USERNAME_MIN_LENGTH, USERNAME_MAX_LENGTH, 
        "Username phải từ " + USERNAME_MIN_LENGTH + " đến " + USERNAME_MAX_LENGTH + " ký tự");
    
    checkPattern(trimmed, USERNAME_PATTERN, "Username chỉ được chứa chữ cái, số, dấu _ và .");

    return null;
  }

  /**
   * Validate password — trả null nếu hợp lệ.
   */
  public static String validatePassword(String password) {
    checkRequireString(password, "Mật khẩu không được để trống");
    checkRequireLength(password, PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH, 
        "Mật khẩu phải từ " + PASSWORD_MIN_LENGTH + " đến " + PASSWORD_MAX_LENGTH + " ký tự");
    
    checkPattern(password, PASSWORD_PATTERN, "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số");
    return null;

  }

  /**
   * Validate email — trả null nếu hợp lệ.
   */
  public static String validateEmail(String email) {
    checkRequireString(email, "Email không được để trống");
    checkPattern(email.trim(), EMAIL_PATTERN, "Email không hợp lệ");

    return null;
  }

  /**
   * Validate số tiền nạp — trả null nếu hợp lệ.
   */
  public static String validateTopUpAmount(BigDecimal amount) {
    checkTopUpAmount(amount);
    checkTopUpAmount(amount, TOPUP_MIN_AMOUNT, TOPUP_MAX_AMOUNT, 
        "Số tiền nạp phải từ " + TOPUP_MIN_AMOUNT + " đến " + TOPUP_MAX_AMOUNT + " VND");
    return null;
  }

  /**
   * Validate Phone - trả về null nếu hợp lệ
   */
  public static String validatePhone(String phone) {
    checkRequireString(phone, "Số điện thoại không được để trống");
    checkRequireLength(phone, 10, 10, "Số điện thoại phải có 10 chữ số");
    return null;
  }

  /**
   * Validate số tiền rút - trả null nếu 
   */
  public static String validateWithdrawAmount(BigDecimal amount) {
    checkTopUpAmount(amount);
    checkTopUpAmount(amount, TOPUP_MIN_AMOUNT, TOPUP_MAX_AMOUNT, 
        "Số tiền rút phải từ " + TOPUP_MIN_AMOUNT + " đến " + TOPUP_MAX_AMOUNT);
    return null;
  }

  // ════════════════════════════════════════════════════
  // HELPER CÁC HÀM NGHIỆP VỤ
  // ════════════════════════════════════════════════════
  private static void checkRequireString(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new UserServiceException(message);
    }
  }

  private static void checkRequireLength(String value, int minLength, int maxLength, String message) {
    if (value.length() < minLength) {
      throw new UserServiceException(message);
    }
    if (value.length() > maxLength) {
      throw new UserServiceException(message);
    }
  }

  private static void checkPattern(String value, Pattern pattern, String message) {
    if (!pattern.matcher(value).matches()) {
      throw new UserServiceException(message);
    }
  }

  private static void checkTopUpAmount(BigDecimal value, BigDecimal min, BigDecimal max, String message) {
    if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
      throw new UserServiceException(message);
    }
  }

  private static void checkTopUpAmount(BigDecimal value) {
    if (value == null) {
      throw new UserServiceException("Số tiền không được để trống");
    }
  }
}