package vn.edu.vnu.uet.group8.client.service;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.LoginRequest;
import vn.edu.vnu.uet.group8.common.dto.LoginResponse;
import vn.edu.vnu.uet.group8.common.dto.ServerRequest;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

import java.util.function.Consumer;

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
     * @param username     tên người dùng
     * @param password  mật khẩu plaintext
     * @param onSuccess callback khi đăng nhập thành công (không tham số)
     * @param onFailure callback khi thất bại, nhận thông báo lỗi từ server
     */
    public static void login(String username, String password, Runnable onSuccess, Consumer<String> onFailure){
        // Validate phía client trước khi gửi - fail fast
        if(username == null || username.isBlank()){
            onFailure.accept("Vui lòng nhập tên đăng nhập");
            return;
        }
        if(password == null || password.isEmpty()){
            onFailure.accept("Vui lòng nhập mật khẩu");
            return;
        }
        if(!AuctionClient.getInstance().isConnected()){
            onFailure.accept("Không có kết nối đến server");
            return;
        }
        LoginRequest payload = LoginRequest.of(username.trim(), password);
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
            LoginResponse loginResp = response.getData(LoginResponse.class);
            if(loginResp == null || !loginResp.isSuccess()){
                String msg = (loginResp != null) ? loginResp.getMessage() : "Phản hồi server không hợp lệ";
                onFailure.accept(msg != null ? msg : "Đăng nhập thất bại");
                return;
            }
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
    }
}
