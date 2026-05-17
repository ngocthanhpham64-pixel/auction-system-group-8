package vn.edu.vnu.uet.group8.client.service;

import java.util.function.Consumer;

import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.common.dto.request.RegisterRequest;
import vn.edu.vnu.uet.group8.common.dto.request.ServerRequest;
import vn.edu.vnu.uet.group8.common.enums.ActionType;

/**
 * RegisterService.
 */
public final class RegisterService {

  private RegisterService() {}

  public static void register(
      String username,
      String email,
      String password,
      String fullName,
      String phone,
      Runnable onSuccess,
      Consumer<String> onFailure) {

    if (!AuctionClient.getInstance().isConnected()) {
      if (!AuctionClient.getInstance().reconnect()) {
        onFailure.accept("Không có kết nối đến server");
        return;
      }
    }

    try {
      RegisterRequest payload =
          RegisterRequest.builder()
              .username(username)
              .email(email)
              .password(password)
              .fullname(fullName)
              .phone(phone)
              .build();

      ServerRequest<RegisterRequest> request =
          ServerRequest.<RegisterRequest>builder(ActionType.REGISTER).payload(payload).build();

      AuctionClient.getInstance()
          .sendRequest(
              request,
              response -> {
                if (response.isSuccess()) {
                  onSuccess.run();
                } else {
                  String msg =
                      response.getMessage() != null ? response.getMessage() : "Đăng ký thất bại";
                  onFailure.accept(msg);
                }
              });
    } catch (IllegalArgumentException e) {
      // Bắt lỗi validate từ RegisterRequest.Builder
      onFailure.accept(e.getMessage());
    }
  }
}