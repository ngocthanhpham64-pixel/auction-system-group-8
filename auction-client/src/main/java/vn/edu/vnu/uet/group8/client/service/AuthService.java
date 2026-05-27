package vn.edu.vnu.uet.group8.client.service;

import java.util.Map;
import java.util.function.Consumer;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.request.LoginRequest;
import vn.edu.vnu.uet.group8.common.dto.request.ServerRequest;
import vn.edu.vnu.uet.group8.common.dto.response.LoginResponse;
import vn.edu.vnu.uet.group8.common.enums.ActionType;

/**
 * Service xử lý xác thực: Đăng nhập, đăng xuất.
 */
public final class AuthService {
  private AuthService(){}
  /**
   * Đăng nhập bất đồng bộ
   * -Gửi (LoginRequest) lên server, nhận LoginResponse
   * Nếu thành công: Lưu session, cập nhật ClientModel, gọi onSuccess
   * Nếu thất bại: gọi onFailure với thông báo lỗi.
   * Callback chạy trên FX Thread - có thể update UI trực tiếp
   * @param email     tên người dùng
   * @param password  mật khẩu plaintext
   * @param onSuccess callback khi đăng nhập thành công (không tham số)
   * @param onFailure callback khi thất bại, nhận thông báo lỗi từ server
   */
  public static void login(
          String email,
          String password,
          Runnable onSuccess,
          Consumer<String> onFailure
  ) {

      // validate email
      if (email == null || email.isBlank()) {
          if (onFailure != null) {
              onFailure.accept("Vui lòng nhập tên đăng nhập");
          }
          return;
      }

      // validate password
      if (password == null || password.isEmpty()) {
          if (onFailure != null) {
              onFailure.accept("Vui lòng nhập mật khẩu");
          }
          return;
      }

      // connection guard
      if (!AuctionClient.getInstance().isConnected()) {
          if (!AuctionClient.getInstance().reconnect()) {
              if (onFailure != null) {
                  onFailure.accept("Không có kết nối đến server");
              }
              return;
          }
      }

      LoginRequest payload =
              LoginRequest.of(email.trim(), password);

      ServerRequest<LoginRequest> request =
              ServerRequest
                      .<LoginRequest>builder(ActionType.LOGIN)
                      .payload(payload)
                      .build();

      AuctionClient.getInstance().sendRequest(request, response -> {

          // login fail
          if (!response.isSuccess()) {

              String msg =
                      response.getMessage() != null
                              ? response.getMessage()
                              : "Đăng nhập thất bại";

              if (onFailure != null) {
                  onFailure.accept(msg);
              }
              return;
          }

          // parse result
          var loginResult = response.getData(
                  vn.edu.vnu.uet.group8.common.dto.model.LoginResultDTO.class
          );

          if (loginResult == null || loginResult.user() == null) {

              if (onFailure != null) {
                  onFailure.accept("Phản hồi server không hợp lệ");
              }
              return;
          }

          String role =
                  loginResult.user().isAdmin()
                          ? "ADMIN"
                          : "MEMBER";

          LoginResponse loginResp =
                  LoginResponse.success(
                          loginResult.user().getUserId(),
                          loginResult.user().getUsername(),
                          loginResult.user().getDisplayName(),
                          null,
                          role,
                          loginResult.token()
                  );

          // save session
          SessionManager.setSession(
                  loginResp.getAuthToken(),
                  loginResp.getUserId(),
                  loginResp.getUsername(),
                  loginResp.getFullName(),
                  loginResp.getRole()
          );

          ClientModel.getInstance().setCurrentUser(loginResp);
          ClientModel.getInstance().setLoggedIn(true);

          // success callback
          if (onSuccess != null) {
              onSuccess.run();
          }
      });
  }
  /**
   * Đăng xuất: gửi LOGOUT request lên server( nếu còn kết nối), sau đó dọn session cục bộ.
   * Tụ động chuyển về màn hình login thông qua SessionManager.logout().
   */
  public static void logout(){
    //Gửi Logout để server invalidate token
    if(AuctionClient.getInstance().isConnected()){
        ServerRequest<Void> logoutReq = ServerRequest
                .<Void>builder(ActionType.LOGOUT)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .build();
        //Gửi request, khi server phản hổi (hoặc lỗi) thì mới dọn session
        AuctionClient.getInstance().sendRequest(logoutReq);
    }
    // Dọn session và chuyển về login dù server có nhận được hay không
    SessionManager.logout();
    ClientModel.getInstance().clearSession();
  }

  /**
   * Gửi yêu cầu mã OTP đến email.
   * @param email Email của tài khoản cần khôi phục
   * @param onSuccess Callback nhận mã OTP (String) nếu thành công
   * @param onFailure Callback nhận thông báo lỗi nếu thất bại
   */
  public static void requestOtp(
          String email,
          Consumer<String> onSuccess,
          Consumer<String> onFailure
  ) {

      if (!AuctionClient.getInstance().isConnected()) {

          if (!AuctionClient.getInstance().reconnect()) {

              if (onFailure != null) {
                  onFailure.accept("Không có kết nối đến server");
              }

              return;
          }
      }

      ServerRequest<Map<String, String>> req =
              ServerRequest
                      .<Map<String, String>>builder(
                              ActionType.valueOf("AUTH_REQUEST_OTP")
                      )
                      .payload(Map.of("email", email))
                      .build();

      AuctionClient.getInstance().sendRequest(req, response -> {

          if (response.isSuccess()) {

              String otp =
                      vn.edu.vnu.uet.group8.common.util.GsonUtil
                              .toObject(response.getData(), String.class);

              if (onSuccess != null) {
                  onSuccess.accept(otp);
              }

          } else {

              if (onFailure != null) {
                  onFailure.accept(response.getMessage());
              }
          }
      });
  }

  /**
   * Đặt lại mật khẩu bằng mã OTP.
   */
  public static void resetPassword(
          String email,
          String otp,
          String newPassword,
          Runnable onSuccess,
          Consumer<String> onFailure
  ) {

      if (!AuctionClient.getInstance().isConnected()) {

          if (onFailure != null) {
              onFailure.accept("Không có kết nối đến server");
          }

          return;
      }

      ServerRequest<Map<String, String>> req =
              ServerRequest
                      .<Map<String, String>>builder(
                              ActionType.valueOf("AUTH_RESET_PASSWORD")
                      )
                      .payload(Map.of(
                              "email", email,
                              "otp", otp,
                              "newPassword", newPassword
                      ))
                      .build();

      AuctionClient.getInstance().sendRequest(req, response -> {

          if (response.isSuccess()) {

              if (onSuccess != null) {
                  onSuccess.run();
              }

          } else {

              if (onFailure != null) {
                  onFailure.accept(response.getMessage());
              }
          }
      });
  }
}
