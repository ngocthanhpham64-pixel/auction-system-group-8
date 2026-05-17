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
  public static void login(String email, String password, Runnable onSuccess, Consumer<String> onFailure){
    // Validate phía client trước khi gửi - fail fast
    if(email == null || email.isBlank()){
        onFailure.accept("Vui lòng nhập tên đăng nhập");
        return;
    }
    if(password == null || password.isEmpty()){
        onFailure.accept("Vui lòng nhập mật khẩu");
        return;
    }
    if(!AuctionClient.getInstance().isConnected()){
        if (!AuctionClient.getInstance().reconnect()) {
            onFailure.accept("Không có kết nối đến server");
            return;
        }
    }
    LoginRequest payload = LoginRequest.of(email.trim(), password);
    ServerRequest<LoginRequest> request = ServerRequest
            .<LoginRequest> builder(ActionType.LOGIN)
            .payload(payload)
            .build();
    AuctionClient.getInstance().sendRequest(request,response -> {
        //Callback này đã ở FX Thread(do ResponseDispatcher đảm bảo)
        if(!response.isSuccess()){
            String msg = response.getMessage() != null
                    ? response.getMessage()
                    : "Đăng nhập thất bại"; 
            onFailure.accept(msg);
            return;
        }
        // Parse dữ liệu bằng getData(Class) - ServerResponse đã có logic convert LinkedTreeMap
        vn.edu.vnu.uet.group8.common.dto.model.LoginResultDTO loginResult = response.getData(
          vn.edu.vnu.uet.group8.common.dto.model.LoginResultDTO.class);
        if(loginResult == null || loginResult.user() == null){
            onFailure.accept("Phản hồi server không hợp lệ");
            return;
        }
        String role = loginResult.user().isAdmin() ? "ADMIN" : "MEMBER";
        LoginResponse loginResp = LoginResponse.success(
                loginResult.user().getUserId(),
                loginResult.user().getUsername(),
                loginResult.user().getDisplayName(),
                null,
                role,
                loginResult.token()
        );
        //Lưu session
        SessionManager.setSession(
                loginResp.getAuthToken(),
                loginResp.getUserId(),
                loginResp.getUsername(),
                loginResp.getFullName(),
                loginResp.getRole());
        // Cập nhật ClientModel
        ClientModel.getInstance().setCurrentUser(loginResp);
        ClientModel.getInstance().setLoggedIn(true);
        // Gọi callback thành công (không tham số)
        onSuccess.run();
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
  public static void requestOtp(String email, Consumer<String> onSuccess, Consumer<String> onFailure) {
    if (!AuctionClient.getInstance().isConnected()) {
      if (!AuctionClient.getInstance().reconnect()) {
        onFailure.accept("Không có kết nối đến server");
        return;
      }
    }

    // Lưu ý: Cần đảm bảo enum ActionType.AUTH_REQUEST_OTP đã được khai báo
    ServerRequest<Map<String, String>> req = ServerRequest.<Map<String, String>>builder(ActionType.valueOf("AUTH_REQUEST_OTP"))
        .payload(Map.of("email", email))
        .build();

    AuctionClient.getInstance().sendRequest(req, response -> {
      if (response.isSuccess()) {
        String otp = vn.edu.vnu.uet.group8.common.util.GsonUtil.toObject(response.getData(), String.class);
        onSuccess.accept(otp); // Thành công, trả về OTP để hiển thị lên màn hình
      } else {
        onFailure.accept(response.getMessage());
      }
    });
  }

  /**
   * Đặt lại mật khẩu bằng mã OTP.
   */
  public static void resetPassword(String email, String otp, String newPassword, Runnable onSuccess, Consumer<String> onFailure) {
    if (!AuctionClient.getInstance().isConnected()) {
      onFailure.accept("Không có kết nối đến server");
      return;
    }

    // Lưu ý: Cần đảm bảo enum ActionType.AUTH_RESET_PASSWORD đã được khai báo
    ServerRequest<Map<String, String>> req = ServerRequest.<Map<String, String>>builder(ActionType.valueOf("AUTH_RESET_PASSWORD"))
        .payload(Map.of("email", email, "otp", otp, "newPassword", newPassword))
        .build();

    AuctionClient.getInstance().sendRequest(req, response -> {
      if (response.isSuccess()) {
        onSuccess.run(); // Thành công
      } else {
        onFailure.accept(response.getMessage());
      }
    });
  }
}
