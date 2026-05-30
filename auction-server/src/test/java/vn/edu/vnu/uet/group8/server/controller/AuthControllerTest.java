package vn.edu.vnu.uet.group8.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonObject;
import vn.edu.vnu.uet.group8.server.service.user.PasswordService;

import vn.edu.vnu.uet.group8.common.dto.model.LoginResultDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserSummaryDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.exception.InvalidCredentialsException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.auth.SessionManager;
import vn.edu.vnu.uet.group8.server.service.user.AuthService;
import vn.edu.vnu.uet.group8.server.service.user.RegisterService;

/**
 * Test cho {@link AuthController}.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthService authService;
  @Mock private RegisterService registerService;
  @Mock private SessionManager sessionManager;
  @Mock private PasswordService passwordService;

  private AuthController controller;

  @BeforeEach
  void setUp() {
    controller = new AuthController(
        authService,
        registerService,
        passwordService,
        sessionManager);
  }

  private UserSummaryDTO taoUserSummary() {
    UserMember m = new UserMember.Builder("alice", "a@e.com", "hash").build();
    m.assignId(1);
    return UserSummaryDTO.from(m);
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleLogin()")
  class LoginTest {

    @Test
    @DisplayName("Login thành công → ServerResponse success=true")
    void loginThanhCong() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("email", "a@e.com");
      payload.addProperty("password", "pass123");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      UserSummaryDTO user = taoUserSummary();
      LoginResultDTO result = LoginResultDTO.from(user, "token-xyz");
      when(authService.login("a@e.com", "pass123")).thenReturn(result);

      ServerResponse res = controller.handleLogin(req, "req-1");

      assertTrue(res.isSuccess());
      assertEquals("LOGIN", res.getAction());
      assertEquals("req-1", res.getRequestId());
      assertNotNull(res.getData());
    }

    @Test
    @DisplayName("Login với payload thiếu → response error")
    void loginThieuPayload() {
      JsonObject req = new JsonObject();

      ServerResponse res = controller.handleLogin(req, "req-1");

      assertFalse(res.isSuccess());
      assertEquals("LOGIN", res.getAction());
      assertTrue(res.getMessage().contains("Thiếu payload"));
    }

    @Test
    @DisplayName("AuthService ném InvalidCredentialsException → response error")
    void loginSaiMatKhau() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("email", "a@e.com");
      payload.addProperty("password", "wrong");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      when(authService.login(anyString(), anyString()))
          .thenThrow(new InvalidCredentialsException());

      ServerResponse res = controller.handleLogin(req, "req-1");

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("Đăng nhập thất bại"));
    }

    @Test
    @DisplayName("AuthService ném ValidationException → response error")
    void loginValidationError() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("email", "");
      payload.addProperty("password", "x");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      when(authService.login(anyString(), anyString()))
          .thenThrow(new ValidationException("Email không được trống"));

      ServerResponse res = controller.handleLogin(req, "req-1");

      assertFalse(res.isSuccess());
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleRegister()")
  class RegisterTest {

    @Test
    @DisplayName("Register thành công → ServerResponse success=true")
    void registerThanhCong() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("username", "alice");
      payload.addProperty("email", "a@e.com");
      payload.addProperty("password", "pass123");
      payload.addProperty("fullname", "Alice");
      payload.addProperty("phone", "0912345678");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      when(registerService.register(anyString(), anyString(), anyString(),
          anyString(), anyString(), any()))
          .thenReturn(taoUserSummary());

      ServerResponse res = controller.handleRegister(req, "req-1");

      assertTrue(res.isSuccess());
      assertEquals("REGISTER", res.getAction());
    }

    @Test
    @DisplayName("Register thiếu payload → response error")
    void registerThieuPayload() {
      JsonObject req = new JsonObject();

      ServerResponse res = controller.handleRegister(req, "req-1");

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("Thiếu payload"));
    }

    @Test
    @DisplayName("RegisterService ném exception → response error")
    void registerLoi() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("username", "alice");
      payload.addProperty("email", "a@e.com");
      payload.addProperty("password", "pass123");
      payload.addProperty("fullname", "Alice");
      payload.addProperty("phone", "0912345678");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      when(registerService.register(any(), any(), any(), any(), any(), any()))
          .thenThrow(new ValidationException("Username đã tồn tại"));

      ServerResponse res = controller.handleRegister(req, "req-1");

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("Đăng ký thất bại"));
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleLogout()")
  class LogoutTest {

    @Test
    @DisplayName("Logout luôn success - invalidate token")
    void logoutThanhCong() {
      ServerResponse res = controller.handleLogout("token-xyz", "req-1");

      assertTrue(res.isSuccess());
      assertEquals("LOGOUT", res.getAction());
      verify(sessionManager).invalidateToken("token-xyz");
    }

    @Test
    @DisplayName("Logout với token null - vẫn không lỗi")
    void logoutTokenNull() {
      ServerResponse res = controller.handleLogout(null, "req-1");

      assertTrue(res.isSuccess());
      verify(sessionManager).invalidateToken(null);
    }
  }

  // ──────────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("handleHeartbeat()")
  class HeartbeatTest {

    @Test
    @DisplayName("Heartbeat trả pong")
    void heartbeat() {
      ServerResponse res = controller.handleHeartbeat("req-1");

      assertTrue(res.isSuccess());
      assertEquals("HEARTBEAT", res.getAction());
      assertEquals("pong", res.getMessage());
    }
  }

  @Nested
  @DisplayName("handleRequestOtp()")
  class RequestOtpTest {
    @Test
    @DisplayName("Yêu cầu OTP thành công")
    void success() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("email", "test@example.com");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      when(passwordService.requestOtpForPasswordReset("test@example.com")).thenReturn("123456");

      ServerResponse res = controller.handleRequestOtp(req, "req-otp");

      assertTrue(res.isSuccess());
      assertEquals("123456", res.getData());
    }

    @Test
    @DisplayName("Yêu cầu OTP thất bại - Email không tồn tại")
    void fail() throws Exception {
      JsonObject req = new JsonObject();
      req.addProperty("email", "unknown@example.com");

      when(passwordService.requestOtpForPasswordReset(anyString())).thenThrow(new RuntimeException("Email không tồn tại"));

      ServerResponse res = controller.handleRequestOtp(req, "req-otp-fail");

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("Email không tồn tại"));
    }
  }

  @Nested
  @DisplayName("handleResetPassword()")
  class ResetPasswordTest {
    @Test
    @DisplayName("Reset mật khẩu thành công")
    void success() throws Exception {
      JsonObject payload = new JsonObject();
      payload.addProperty("email", "test@example.com");
      payload.addProperty("otp", "123456");
      payload.addProperty("newPassword", "newPass123");
      JsonObject req = new JsonObject();
      req.add("payload", payload);

      ServerResponse res = controller.handleResetPassword(req, "req-reset");

      assertTrue(res.isSuccess());
      verify(passwordService).resetPasswordWithOtp("test@example.com", "123456", "newPass123");
    }

    @Test
    @DisplayName("Reset mật khẩu thất bại - OTP sai")
    void invalidOtp() throws Exception {
      JsonObject req = new JsonObject();
      req.addProperty("email", "test@example.com");
      req.addProperty("otp", "000000");
      req.addProperty("newPassword", "newPass123");

      doThrow(new RuntimeException("OTP không hợp lệ")).when(passwordService)
          .resetPasswordWithOtp(anyString(), anyString(), anyString());

      ServerResponse res = controller.handleResetPassword(req, "req-reset-fail");

      assertFalse(res.isSuccess());
      assertTrue(res.getMessage().contains("OTP không hợp lệ"));
    }
  }
}