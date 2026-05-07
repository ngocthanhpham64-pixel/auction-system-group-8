package vn.edu.vnu.uet.group8.server.service.user;

import java.sql.SQLException;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.exception.InvalidCredentialsException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.user.UserPolicy;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

/**
 * Xử lý nghiệp vụ liên quan đến mật khẩu.
 */
public class PasswordService {

  private final UserDAO userDAO;

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

    userDAO.updatePassword(userId, newPassword);
  }
}