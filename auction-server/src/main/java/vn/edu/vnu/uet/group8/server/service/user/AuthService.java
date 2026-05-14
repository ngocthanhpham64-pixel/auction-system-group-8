package vn.edu.vnu.uet.group8.server.service.user;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.model.LoginResultDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserSummaryDTO;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.AccountLockedException;
import vn.edu.vnu.uet.group8.common.exception.InvalidCredentialsException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.auth.SessionManager;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;


/**
 * Xử lý nghiệp vụ đăng nhập và xác thực.
 */
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserDAO userDAO;
  private final SessionManager sessionManager;

  public AuthService(UserDAO userDAO, SessionManager sessionManager) {
    this.userDAO = userDAO;
    this.sessionManager = sessionManager;
  }

  /**
   * Đăng nhập bằng email và password.
   *
   * <p>Trả cùng một message cho cả "email sai" và "password sai"
   * để tránh lộ thông tin "email này có tồn tại trong hệ thống không".
   *
   * @return {@link LoginResultDTO} nếu thành công
   * @throws ValidationException         nếu email hoặc password để trống
   * @throws InvalidCredentialsException nếu email hoặc password sai
   * @throws AccountLockedException      nếu tài khoản bị khoá hoặc cấm
   * @throws SQLException                nếu lỗi DB
   */
  public LoginResultDTO login(String email, String password)
      throws SQLException {

    // -- Bước 1: Validate input cơ bản
    if (email == null || email.isBlank()) {
      throw new ValidationException("Email không được để trống");
    }
    if (password == null || password.isBlank()) {
      throw new ValidationException("Mật khẩu không được để trống");
    }

    // -- Bước 2: Tìm user bằng email
    // Service chịu trách nhiệm cho logic "tìm -> xác thực -> kiểm tra"
    // thay vì đẩy hết cho một hàm authenticate() lớn trong DAO.
    User user = userDAO.findByEmail(email.trim().toLowerCase())
        .orElseThrow(InvalidCredentialsException::new);

    // -- Bước 3: Xác thực mật khẩu
    // Dùng InvalidCredentialsException cho cả "không tìm thấy user" và "sai mật khẩu"
    // để tránh user enumeration attack.
    if (!PasswordUtil.verify(password, user.getEncryptedPassword())) {
      throw new InvalidCredentialsException();
    }

    // -- Bước 4: Kiểm tra trạng thái tài khoản
    // Chỉ thực hiện sau khi đã xác thực thành công để không lộ thông tin
    // để không lộ "username này có tồn tại không"
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new AccountLockedException(user.getStatus());
    }

    // -- Bước 5: Ghi nhận lastLogin (không làm gián đoạn luồng nếu lỗi)
    recordLoginSilently(user);

    String token = sessionManager.createSession(user.getId());
    UserSummaryDTO userSummary = UserSummaryDTO.from(user);

    return new LoginResultDTO(userSummary, token);
  }

  /** Ghi nhận lastLogin, bỏ qua nếu lỗi — không chặn luồng login. */
  private void recordLoginSilently(User user) {
    try {
      userDAO.updateLastLogin(user.getId());
      user.recordLogin();
    } catch (SQLException e) {
      log.warn("Không ghi được lastLogin cho userId=" + user.getId());
    }
  }
}