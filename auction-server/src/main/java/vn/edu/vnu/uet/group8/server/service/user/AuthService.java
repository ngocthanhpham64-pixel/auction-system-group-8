package vn.edu.vnu.uet.group8.server.service.user;

import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.dto.model.UserSummaryDTO;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.AccountLockedException;
import vn.edu.vnu.uet.group8.common.exception.InvalidCredentialsException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;


/**
 * Xử lý nghiệp vụ đăng nhập và xác thực.
 */
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserDAO userDAO;

  public AuthService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  /**
   * Đăng nhập bằng email và password.
   *
   * <p>Trả cùng một message cho cả "email sai" và "password sai"
   * để tránh lộ thông tin "email này có tồn tại trong hệ thống không".
   *
   * @return {@link UserSummaryDTO} nếu thành công
   * @throws ValidationException         nếu email hoặc password để trống
   * @throws InvalidCredentialsException nếu email hoặc password sai
   * @throws AccountLockedException      nếu tài khoản bị khoá hoặc cấm
   * @throws SQLException                nếu lỗi DB
   */
  public UserSummaryDTO login(String username, String password)
      throws SQLException {

    // -- Kiểm tra trống -- không dùng UserPolicy vì đây không phải
    // kiểm tra format, chỉ kiểm tra có nhập hay không
    if (username == null || username.isBlank()) {
      throw new ValidationException("Username không được để trống");
    }
    if (password == null || password.isBlank()) {
      throw new ValidationException("Mật khẩu không được để trống");
    }

    // -- Xác thực -- DAO tự BCrypt verify bên trong
    Optional<User> opt =
        userDAO.authenticate(username.trim().toLowerCase(), password);

    if (opt.isEmpty()) {
      throw new InvalidCredentialsException();
    }

    User user = opt.get();

    // -- Kiểm tra trạng thái -- sau khi verify password thành công
    // để không lộ "username này có tồn tại không"
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new AccountLockedException(user.getStatus());
    }

    // -- Ghi nhận lastLogin -- không fail login nếu bước này lỗi
    recordLoginSilently(user);

    return UserSummaryDTO.from(user);
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