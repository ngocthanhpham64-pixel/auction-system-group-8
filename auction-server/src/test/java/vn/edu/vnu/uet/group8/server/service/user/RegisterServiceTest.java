package vn.edu.vnu.uet.group8.server.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.dto.model.UserSummaryDTO;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.exception.UserServiceException;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

  @Mock private UserDAO userDAO;

  @InjectMocks private RegisterService registerService;

  private static final String USERNAME = "quan123";
  private static final String EMAIL = "quan@example.com";
  private static final String PASSWORD = "Secret123";
  private static final String FULLNAME = "Pham Anh Quan";
  private static final String PHONE = "0912345678";
  private static final String AVATAR = "";

  @Nested
  @DisplayName("Validate input format")
  class ValidateInput {

    @Test
    @DisplayName("Username không hợp lệ")
    void usernameKhongHopLe() {
      assertThrows(
          UserServiceException.class,
          () -> registerService.register("ab", EMAIL, PASSWORD, FULLNAME, PHONE, AVATAR));
    }

    @Test
    @DisplayName("Email không hợp lệ")
    void emailKhongHopLe() {
      assertThrows(
          UserServiceException.class,
          () -> registerService.register(USERNAME, "notanemail", PASSWORD, FULLNAME, PHONE, AVATAR));
    }

    @Test
    @DisplayName("Password không hợp lệ")
    void passwordKhongHopLe() {
      assertThrows(
          UserServiceException.class,
          () -> registerService.register(USERNAME, EMAIL, "12", FULLNAME, PHONE, AVATAR));
    }

    @Test
    @DisplayName("Phone không hợp lệ")
    void phoneKhongHopLe() {
      assertThrows(
          UserServiceException.class,
          () -> registerService.register(USERNAME, EMAIL, PASSWORD, FULLNAME, "abc", AVATAR));
    }
  }

  @Nested
  @DisplayName("Check duplicate")
  class CheckDuplicate {

    @Test
    @DisplayName("Username đã tồn tại")
    void usernameTrung() throws SQLException {
      when(userDAO.existsByUsername(USERNAME)).thenReturn(true);

      assertThrows(
          UserServiceException.class,
          () -> registerService.register(USERNAME, EMAIL, PASSWORD, FULLNAME, PHONE, AVATAR));
    }

    @Test
    @DisplayName("Email đã tồn tại")
    void emailTrung() throws SQLException {
      when(userDAO.existsByUsername(USERNAME)).thenReturn(false);
      when(userDAO.existsByEmail(EMAIL)).thenReturn(true);

      assertThrows(
          UserServiceException.class,
          () -> registerService.register(USERNAME, EMAIL, PASSWORD, FULLNAME, PHONE, AVATAR));
    }

    @Test
    @DisplayName("Phone đã tồn tại")
    void phoneTrung() throws SQLException {
      when(userDAO.existsByUsername(USERNAME)).thenReturn(false);
      when(userDAO.existsByEmail(EMAIL)).thenReturn(false);
      when(userDAO.existsByPhone(PHONE)).thenReturn(true);

      assertThrows(
          UserServiceException.class,
          () -> registerService.register(USERNAME, EMAIL, PASSWORD, FULLNAME, PHONE, AVATAR));
    }
  }

  @Nested
  @DisplayName("Register success")
  class Success {

    @Test
    @DisplayName("Đăng ký thành công")
    void thanhCong() throws SQLException {
      when(userDAO.existsByUsername(USERNAME)).thenReturn(false);
      when(userDAO.existsByEmail(EMAIL)).thenReturn(false);
      when(userDAO.existsByPhone(PHONE)).thenReturn(false);

      doAnswer(
              invocation -> {
                UserMember user = invocation.getArgument(0);
                user.assignId(42);
                return null;
              })
          .when(userDAO)
          .insert(any(UserMember.class));

      UserSummaryDTO dto =
          registerService.register(USERNAME, EMAIL, PASSWORD, FULLNAME, PHONE, AVATAR);

      assertNotNull(dto);
      assertEquals(USERNAME, dto.getUsername());

      verify(userDAO).insert(any(UserMember.class));
    }

    @Test
    @DisplayName("Normalize username + email")
    void normalize() throws SQLException {
      when(userDAO.existsByUsername("quan123")).thenReturn(false);
      when(userDAO.existsByEmail("quan@example.com")).thenReturn(false);
      when(userDAO.existsByPhone(PHONE)).thenReturn(false);

      doAnswer(
              invocation -> {
                UserMember user = invocation.getArgument(0);
                user.assignId(1);
                return null;
              })
          .when(userDAO)
          .insert(any(UserMember.class));

      registerService.register(
          "  QUAN123  ", "  QUAN@EXAMPLE.COM  ", PASSWORD, FULLNAME, PHONE, AVATAR);

      verify(userDAO).existsByUsername("quan123");
      verify(userDAO).existsByEmail("quan@example.com");
    }

    @Test
    @DisplayName("Fullname null")
    void fullnameNull() throws SQLException {
      when(userDAO.existsByUsername(USERNAME)).thenReturn(false);
      when(userDAO.existsByEmail(EMAIL)).thenReturn(false);
      when(userDAO.existsByPhone(PHONE)).thenReturn(false);

      doAnswer(
              invocation -> {
                UserMember user = invocation.getArgument(0);
                user.assignId(1);
                return null;
              })
          .when(userDAO)
          .insert(any(UserMember.class));

      assertDoesNotThrow(
          () -> registerService.register(USERNAME, EMAIL, PASSWORD, null, PHONE, AVATAR));
    }
  }
}