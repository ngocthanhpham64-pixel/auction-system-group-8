package vn.edu.vnu.uet.group8.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.LoginResultDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserSummaryDTO;
import vn.edu.vnu.uet.group8.common.dto.request.LoginRequest;
import vn.edu.vnu.uet.group8.common.dto.request.RegisterRequest;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.auth.SessionManager;
import vn.edu.vnu.uet.group8.server.network.RequestParser;
import vn.edu.vnu.uet.group8.server.service.user.AuthService;
import vn.edu.vnu.uet.group8.server.service.user.PasswordService;
import vn.edu.vnu.uet.group8.server.service.user.RegisterService;

/**
 * Controller cho domain Auth: LOGIN, REGISTER, LOGOUT, HEARTBEAT.
 *
 * <p>Mỗi handle*() nhận JsonObject thô và requestId, trả về ServerResponse.
 * Không touch socket, không touch GSON serialization (đó là việc của ClientHandler).
 *
 * <p>Tất cả exception (validation, DB...) đều bắt ở đây để biến thành
 * error response — caller không phải lo.
 */
public class AuthController {
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthService authService;
  private final RegisterService registerService;
  private final PasswordService passwordService;
  private final SessionManager sessionManager;

  public AuthController(AuthService authService,
                        RegisterService registerService,
                        PasswordService passwordService,
                        SessionManager sessionManager) {
    this.authService = authService;
    this.registerService = registerService;
    this.passwordService = passwordService;
    this.sessionManager = sessionManager;
  }

  /**
   * LOGIN — anonymous request.
   * Tạo token mới nếu thành công, đính kèm vào response.
   *
   * @return ServerResponse, kèm token và userId trong data nếu success
   */
  public ServerResponse handleLogin(JsonObject request, String requestId) {
    try {
      LoginRequest payload = RequestParser.getPayload(request, LoginRequest.class);
      if (payload == null) {
        return ServerResponse.replyError("LOGIN", requestId, "Thiếu payload");
      }

      LoginResultDTO result = authService.login(payload.getEmail(), payload.getPassword());

      // Log userId thật từ kết quả trả về
      log.info("Login thành công: userId={}", result.getUserId());
      return ServerResponse.reply("LOGIN", requestId)
          .success(true)
          .message("Đăng nhập thành công")
          .data(result)
          .build();

    } catch (Exception e) {
      log.warn("Login thất bại: {}", e.getMessage());
      return ServerResponse.replyError("LOGIN", requestId,
          "Đăng nhập thất bại: " + e.getMessage());
    }
  }

  /**
   * REGISTER — anonymous request.
   * Sau khi register thành công, không auto-login: client phải LOGIN.
   */
  public ServerResponse handleRegister(JsonObject request, String requestId) {
    try {
      RegisterRequest payload = RequestParser.getPayload(request, RegisterRequest.class);
      if (payload == null) {
        return ServerResponse.replyError("REGISTER", requestId, "Thiếu payload");
      }

      UserSummaryDTO user = registerService.register(
          payload.getUsername(),
          payload.getEmail(),
          payload.getPassword(),
          payload.getFullname(),
          payload.getPhone(),
          null /* avatarUrl - chưa có trong RegisterRequest */);

      log.info("Register thành công: userId={}", user.getUserId());
      return ServerResponse.reply("REGISTER", requestId)
          .success(true)
          .message("Đăng ký thành công")
          .data(user)
          .build();

    } catch (Exception e) {
      log.warn("Register thất bại: {}", e.getMessage());
      return ServerResponse.replyError("REGISTER", requestId,
          "Đăng ký thất bại: " + e.getMessage());
    }
  }

  /**
   * LOGOUT — authenticated request.
   * Vô hiệu hoá token, sau đó client coi như anonymous.
   */
  public ServerResponse handleLogout(String token, String requestId) {
    sessionManager.invalidateToken(token);
    return ServerResponse.reply("LOGOUT", requestId)
        .success(true)
        .message("Đăng xuất thành công")
        .build();
  }

  /**
   * HEARTBEAT — keep-alive, không cần xác thực sâu.
   * Chỉ trả pong để client biết connection còn sống.
   */
  public ServerResponse handleHeartbeat(String requestId) {
    return ServerResponse.reply("HEARTBEAT", requestId)
        .success(true)
        .message("pong")
        .build();
  }

  /**
   * AUTH_REQUEST_OTP — Yêu cầu gửi mã OTP để đặt lại mật khẩu.
   */
  public ServerResponse handleRequestOtp(JsonObject request, String requestId) {
    try {
      JsonObject payload = request.has("payload") && request.get("payload").isJsonObject() 
          ? request.getAsJsonObject("payload") : request;
      String email = RequestParser.requireString(payload, "email");
      String otp = passwordService.requestOtpForPasswordReset(email);

      log.info("Đã xử lý yêu cầu gửi OTP cho email: {}", email);
      return ServerResponse.reply("AUTH_REQUEST_OTP", requestId)
          .success(true)
          .message("Mã OTP đã được gửi đến email. (Hãy xem trong Console của Server)")
          .data(otp)
          .build();
    } catch (Exception e) {
      return ServerResponse.replyError("AUTH_REQUEST_OTP", requestId, e.getMessage());
    }
  }

  /**
   * AUTH_RESET_PASSWORD — Đặt lại mật khẩu bằng mã OTP.
   */
  public ServerResponse handleResetPassword(JsonObject request, String requestId) {
    try {
      JsonObject payload = request.has("payload") && request.get("payload").isJsonObject() 
          ? request.getAsJsonObject("payload") : request;
      String email = RequestParser.requireString(payload, "email");
      String otp = RequestParser.requireString(payload, "otp");
      String newPassword = RequestParser.requireString(payload, "newPassword");
      
      passwordService.resetPasswordWithOtp(email, otp, newPassword);
      
      log.info("Email {} đã đặt lại mật khẩu thành công bằng OTP", email);

      return ServerResponse.reply("AUTH_RESET_PASSWORD", requestId)
          .success(true)
          .message("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập ngay bây giờ.")
          .build();
    } catch (Exception e) {
      return ServerResponse.replyError("AUTH_RESET_PASSWORD", requestId, e.getMessage());
    }
  }
}
