package vn.edu.vnu.uet.group8.server.service.user;

import java.sql.SQLException;

import vn.edu.vnu.uet.group8.common.dto.model.UserSummaryDTO;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.exception.DuplicateUserException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

/**
 * Xử lý nghiệp vụ đăng ký tài khoản mới.
 *
 * <p>
 * Ném exception khi có lỗi — không trả {@code ResponseDTO}.
 * {@code ClientHandler} bắt exception và tự build response.
 */
public class RegisterService {

  private final UserDAO userDAO;

  public RegisterService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  /**
   * Đăng ký tài khoản mới.
   *
   * @param username  tên đăng nhập
   * @param email     địa chỉ email
   * @param password  mật khẩu plain-text — sẽ được hash trước khi lưu
   * @param fullName
   * @param phone
   * @param avatarUrl
   * @return {@link UserSummaryDTO} để Client tự động login sau đăng ký
   * @throws vn.edu.vnu.uet.group8.common.exception.ValidationException
   *                                                                    nếu input
   *                                                                    không hợp
   *                                                                    lệ
   * @throws DuplicateUserException                                     nếu
   *                                                                    username
   *                                                                    hoặc email
   *                                                                    đã tồn tại
   * @throws SQLException                                               nếu lỗi DB
   */
  public UserSummaryDTO register(
      String username, String email, String password, String fullname,
      String phone, String avatarUrl)
      throws SQLException {

    // -- Validate format -- UserPolicy tự ném ValidationException nếu sai
    UserPolicy.validateUsername(username);
    UserPolicy.validateEmail(email);
    UserPolicy.validatePassword(password);
    UserPolicy.validatePhone(phone);

    // -- Normalize input
    String normalUsername = username.trim().toLowerCase();
    String normalEmail = email.trim().toLowerCase();
    String normalPhone = phone.trim();

    // -- Kiểm tra trùng
    if (userDAO.existsByUsername(normalUsername)) {
      throw new DuplicateUserException("Username", normalUsername);
    }
    if (userDAO.existsByEmail(normalEmail)) {
      throw new DuplicateUserException("Email", normalEmail);
    }
    if (userDAO.existsByPhone(normalPhone)) {
      throw new DuplicateUserException("Phone", normalPhone);
    }

    // -- Tạo entity và lưu DB
    String hashedPassword = PasswordUtil.hash(password);
    UserMember newUser = UserMember.builder(normalUsername, normalEmail, hashedPassword)
        .fullname(fullname != null ? fullname.trim() : "")
        .phone(normalPhone)
        .build();

    userDAO.insert(newUser);
    // newUser.getId() > 0 sau khi insert — DAO đã gán AUTO_INCREMENT

    return UserSummaryDTO.from(newUser);
  }
}