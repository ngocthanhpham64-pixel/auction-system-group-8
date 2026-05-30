package vn.edu.vnu.uet.group8.server.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.dto.model.LoginResultDTO;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.AccountLockedException;
import vn.edu.vnu.uet.group8.common.exception.InvalidCredentialsException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.auth.SessionManager;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.util.PasswordUtil;

/**
 * Test cho {@link AuthService}.
 *
 * <p>Mock UserDAO + SessionManager. PasswordUtil là static method, không mock,
 * dùng hash thật để verify đối với password thật.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserDAO userDAO;

  @Mock private SessionManager sessionManager;

  @InjectMocks private AuthService authService;

  /** Helper - tạo UserMember đã hash password. */
  private UserMember taoMember(String email, String password, UserStatus status) {
    UserMember m =
        new UserMember.Builder("alice", email, PasswordUtil.hash(password))
            .fullname("Alice")
            .build();
    m.assignId(1);
    if (status != UserStatus.ACTIVE) {
      m.setStatus(status);
    }
    return m;
  }

  @Nested
  @DisplayName("Validate input")
  class ValidateInput {

    @Test
    @DisplayName("Email null → ValidationException")
    void emailNull() {
      ValidationException ex =
          assertThrows(ValidationException.class, () -> authService.login(null, "pass"));
      assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    @DisplayName("Email blank → ValidationException")
    void emailBlank() {
      assertThrows(ValidationException.class, () -> authService.login("   ", "pass"));
    }

    @Test
    @DisplayName("Password null → ValidationException")
    void passwordNull() {
      ValidationException ex =
          assertThrows(
              ValidationException.class, () -> authService.login("a@b.com", null));
      assertTrue(
          ex.getMessage().toLowerCase().contains("mật khẩu")
              || ex.getMessage().toLowerCase().contains("password"));
    }

    @Test
    @DisplayName("Password blank → ValidationException")
    void passwordBlank() {
      assertThrows(
          ValidationException.class, () -> authService.login("a@b.com", "   "));
    }
  }

  @Nested
  @DisplayName("Authenticate")
  class Authenticate {

    @Test
    @DisplayName("Email không tồn tại → InvalidCredentialsException")
    void emailKhongTonTai() throws SQLException {
      when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.empty());
      assertThrows(
          InvalidCredentialsException.class, () -> authService.login("a@b.com", "pass"));
    }

    @Test
    @DisplayName("Password sai → InvalidCredentialsException")
    void passwordSai() throws SQLException {
      User user = taoMember("a@b.com", "rightpass", UserStatus.ACTIVE);
      when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.of(user));

      assertThrows(
          InvalidCredentialsException.class, () -> authService.login("a@b.com", "wrongpass"));
    }

    @Test
    @DisplayName("Login thành công → trả LoginResultDTO có token")
    void loginThanhCong() throws SQLException {
      User user = taoMember("a@b.com", "rightpass", UserStatus.ACTIVE);
      when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.of(user));
      when(sessionManager.createSession(1)).thenReturn("token-xyz");

      LoginResultDTO result = authService.login("a@b.com", "rightpass");

      assertNotNull(result);
      assertEquals("token-xyz", result.token());
      assertEquals(1, result.user().getUserId());

      // Verify đã ghi lastLogin
      verify(userDAO).updateLastLogin(1);
      verify(sessionManager).createSession(1);
    }

    @Test
    @DisplayName("Email được normalize (trim + lowercase) trước khi query")
    void emailNormalize() throws SQLException {
      User user = taoMember("a@b.com", "pass", UserStatus.ACTIVE);
      when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.of(user));
      when(sessionManager.createSession(anyInt())).thenReturn("token");

      authService.login("  A@B.COM  ", "pass");

      verify(userDAO).findByEmail("a@b.com");
    }
  }

  @Nested
  @DisplayName("Trạng thái tài khoản")
  class AccountStatus {

    @Test
    @DisplayName("Status SUSPENDED → AccountLockedException")
    void suspended() throws SQLException {
      User user = taoMember("a@b.com", "pass", UserStatus.SUSPENDED);
      when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.of(user));

      AccountLockedException ex =
          assertThrows(
              AccountLockedException.class, () -> authService.login("a@b.com", "pass"));
      assertEquals(UserStatus.SUSPENDED, ex.getStatus());
    }

    @Test
    @DisplayName("Status BANNED → AccountLockedException")
    void banned() throws SQLException {
      User user = taoMember("a@b.com", "pass", UserStatus.BANNED);
      when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.of(user));

      AccountLockedException ex =
          assertThrows(
              AccountLockedException.class, () -> authService.login("a@b.com", "pass"));
      assertEquals(UserStatus.BANNED, ex.getStatus());
    }
  }

  @Nested
  @DisplayName("Resilience")
  class Resilience {

    @Test
    @DisplayName("updateLastLogin ném SQLException - KHÔNG chặn luồng login")
    void lastLoginLoiKhongChan() throws SQLException {
      User user = taoMember("a@b.com", "pass", UserStatus.ACTIVE);
      when(userDAO.findByEmail("a@b.com")).thenReturn(Optional.of(user));
      doThrow(new SQLException("DB lỗi")).when(userDAO).updateLastLogin(anyInt());
      when(sessionManager.createSession(anyInt())).thenReturn("token");

      // Login vẫn thành công dù updateLastLogin fail
      LoginResultDTO result =
          assertDoesNotThrow(() -> authService.login("a@b.com", "pass"));
      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Security - chống user enumeration")
  class SecurityTest {

    @Test
    @DisplayName("Email sai và password sai → CÙNG message")
    void cungMessageChongEnum() throws SQLException {
      // Case 1: email không tồn tại
      when(userDAO.findByEmail("notexist@b.com")).thenReturn(Optional.empty());
      InvalidCredentialsException ex1 =
          assertThrows(
              InvalidCredentialsException.class,
              () -> authService.login("notexist@b.com", "pass"));

      // Case 2: email đúng, password sai
      User user = taoMember("exist@b.com", "right", UserStatus.ACTIVE);
      when(userDAO.findByEmail("exist@b.com")).thenReturn(Optional.of(user));
      InvalidCredentialsException ex2 =
          assertThrows(
              InvalidCredentialsException.class, () -> authService.login("exist@b.com", "wrong"));

      assertEquals(
          ex1.getMessage(),
          ex2.getMessage(),
          "Phải dùng cùng message để chống user enumeration attack");
    }
  }
}