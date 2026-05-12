package vn.edu.vnu.uet.group8.client.service;

import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.common.dto.ServerRequest;
import vn.edu.vnu.uet.group8.common.enums.ActionType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * RegisterService — FE side.
 *
 * Workaround: BE chưa có RegisterRequest DTO + UserSummaryDTO.
 * Tạm dùng Map<String, String> payload, đợi BE bổ sung DTO chính thức.
 */
public final class RegisterService {

    private RegisterService() {}

    public static void register(String username, String email, String password,
                                String fullName, String phone,
                                Runnable onSuccess,
                                Consumer<String> onFailure) {

        if (username == null || username.isBlank()) {
            onFailure.accept("Tên đăng nhập không được trống");
            return;
        }
        if (email == null || email.isBlank()) {
            onFailure.accept("Email không được trống");
            return;
        }
        if (password == null || password.length() < 8) {
            onFailure.accept("Mật khẩu phải có ít nhất 8 ký tự");
            return;
        }
        if (fullName == null || fullName.isBlank()) {
            onFailure.accept("Họ tên không được trống");
            return;
        }
        if (phone == null || phone.isBlank()) {
            onFailure.accept("Số điện thoại không được trống");
            return;
        }
        if (!AuctionClient.getInstance().isConnected()) {
            onFailure.accept("Không có kết nối đến server");
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("username", username.trim().toLowerCase());
        payload.put("email", email.trim().toLowerCase());
        payload.put("password", password);
        payload.put("fullName", fullName.trim());
        payload.put("phone", phone.trim());

        ServerRequest<Map<String, String>> request = ServerRequest
                .<Map<String, String>>builder(ActionType.REGISTER)
                .payload(payload)
                .build();

        AuctionClient.getInstance().sendRequest(request, response -> {
            if (response.isSuccess()) {
                onSuccess.run();
            } else {
                String msg = response.getMessage() != null
                        ? response.getMessage()
                        : "Đăng ký thất bại";
                onFailure.accept(msg);
            }
        });
    }
}