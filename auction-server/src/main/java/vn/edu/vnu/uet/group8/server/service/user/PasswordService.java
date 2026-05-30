package vn.edu.vnu.uet.group8.server.service.user;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.exception.InvalidCredentialsException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

/**
 * Xử lý nghiệp vụ liên quan đến mật khẩu.
 */
public class PasswordService {

  private static final Logger log = LoggerFactory.getLogger(PasswordService.class);
  private final UserDAO userDAO;
  private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();

  private static class OtpData {
    String otp;
    Instant expiry;
    OtpData(String otp, Instant expiry) {
      this.otp = otp;
      this.expiry = expiry;
    }
  }

  public PasswordService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  /**
   * Đổi mật khẩu — yêu cầu nhập mật khẩu cũ để xác nhận danh tính.
   *
   * @throws UserNotFoundException       nếu user không tồn tại
   * @throws InvalidCredentialsException nếu mật khẩu cũ sai
   * @throws ValidationException         nếu mật khẩu mới không hợp lệ
   * @throws SQLException                nếu lỗi DB
   */
  public void changePassword(int userId, String oldPassword, String newPassword)
      throws SQLException {

    User user =
        userDAO
            .findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

    // -- Verify mật khẩu cũ
    if (!PasswordUtil.verify(oldPassword, user.getEncryptedPassword())) {
      throw new InvalidCredentialsException();
    }

    // -- Validate mật khẩu mới -- UserPolicy tự ném nếu sai
    UserPolicy.validatePassword(newPassword);

    // -- Không cho đặt lại mật khẩu giống cũ
    if (PasswordUtil.verify(newPassword, user.getEncryptedPassword())) {
      throw new ValidationException("Mật khẩu mới phải khác mật khẩu cũ");
    }

    // -- DAO đã tự hash, không cần hash lại ở đây
    userDAO.updatePassword(userId, newPassword);
  }

  public String requestOtpForPasswordReset(String email) throws SQLException {
    String normalizedEmail = email.trim().toLowerCase();
    User user = userDAO.findByEmail(normalizedEmail)
        .orElseThrow(() -> new ValidationException("Email không tồn tại trong hệ thống"));

    // Sinh ngẫu nhiên 6 chữ số OTP
    String otp = String.format("%06d", new Random().nextInt(1000000));
    
    // Lưu vào bộ nhớ tạm với thời hạn 5 phút
    otpStore.put(normalizedEmail, new OtpData(otp, Instant.now().plus(5, ChronoUnit.MINUTES)));
    
    // MÔ PHỎNG: In nội dung Email ra màn hình Console của Server thay vì gửi thật
    System.out.println("\n=======================================================");
    System.out.println("[MOCK EMAIL] YÊU CẦU KHÔI PHỤC MẬT KHẨU");
    System.out.println("Gửi đến: " + user.getEmail());
    System.out.println("Mã OTP của bạn là: " + otp);
    System.out.println("Mã này sẽ hết hạn sau 5 phút.");
    System.out.println("=======================================================\n");
    
    log.info("Đã tạo mã OTP cho tài khoản {}", normalizedEmail);
    return otp;
  }

  public void resetPasswordWithOtp(String email, String otp, String newPassword) throws SQLException {
    String normalizedEmail = email.trim().toLowerCase();
    OtpData otpData = otpStore.get(normalizedEmail);
    
    if (otpData == null) {
      throw new ValidationException("Mã OTP không hợp lệ hoặc chưa được yêu cầu.");
    }
    
    if (Instant.now().isAfter(otpData.expiry)) {
      otpStore.remove(normalizedEmail);
      throw new ValidationException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
    }
    
    if (!otpData.otp.equals(otp)) {
      throw new ValidationException("Mã OTP không chính xác.");
    }
    
    UserPolicy.validatePassword(newPassword);
    
    User user = userDAO.findByEmail(normalizedEmail)
        .orElseThrow(() -> new ValidationException("Email không tồn tại trong hệ thống"));
        
    userDAO.updatePassword(user.getId(), newPassword);
    
    otpStore.remove(normalizedEmail); // Xóa OTP sau khi dùng thành công
    log.info("User {} đã đặt lại mật khẩu thành công qua OTP", user.getId());
  }
}