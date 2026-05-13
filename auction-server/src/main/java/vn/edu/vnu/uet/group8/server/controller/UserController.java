package vn.edu.vnu.uet.group8.server.controller;

import com.google.gson.JsonObject;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.PaymentMethod;
import vn.edu.vnu.uet.group8.server.network.RequestParser;
import vn.edu.vnu.uet.group8.server.service.user.BalanceService;
import vn.edu.vnu.uet.group8.server.service.user.ProfileService;

/**
 * Controller cho domain User: USER_PROFILE, USER_DEPOSIT, USER_BIDS.
 *
 * <p>USER_BIDS hiện chưa có service tương ứng — placeholder.
 */
public class UserController {
  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  private final ProfileService profileService;
  private final BalanceService balanceService;

  public UserController(ProfileService profileService,
                        BalanceService balanceService) {
    this.profileService = profileService;
    this.balanceService = balanceService;
  }

  /**
   * USER_PROFILE — lấy profile của bản thân hoặc user khác.
   * Nếu payload không có targetUserId → trả profile của chính người gọi.
   */
  public ServerResponse handleGetProfile(JsonObject request, String requestId,
                                         int authenticatedUserId) {
    try {
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
          ? request.getAsJsonObject("payload") : new JsonObject();

      Integer targetIdOpt = RequestParser.optionalInt(payload, "targetUserId");
      int targetId = targetIdOpt != null ? targetIdOpt : authenticatedUserId;

      UserProfileDTO profile = profileService.getProfile(authenticatedUserId, targetId);

      return ServerResponse.reply("USER_PROFILE", requestId)
          .success(true)
          .message("Lấy profile thành công")
          .data(profile)
          .build();

    } catch (Exception e) {
      log.warn("USER_PROFILE thất bại: {}", e.getMessage());
      return ServerResponse.replyError("USER_PROFILE", requestId,
          "Lỗi: " + e.getMessage());
    }
  }

  /**
   * USER_DEPOSIT — nạp tiền.
   *
   * <p>Payload mong đợi: {amount, transactionId, paymentMethod}
   */
  public ServerResponse handleDeposit(JsonObject request, String requestId,
                                      int authenticatedUserId) {
    try {
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
          ? request.getAsJsonObject("payload") : new JsonObject();

      String amountStr = RequestParser.requireString(payload, "amount");
      String transactionId = RequestParser.requireString(payload, "transactionId");
      String paymentMethodStr = RequestParser.requireString(payload, "paymentMethod");

      BigDecimal amount = new BigDecimal(amountStr);
      PaymentMethod method = PaymentMethod.valueOf(paymentMethodStr);

      BigDecimal newBalance = balanceService.topUpBalance(
          authenticatedUserId, amount, transactionId, method);

      JsonObject data = new JsonObject();
      data.addProperty("newBalance", newBalance.toPlainString());

      log.info("User {} nạp {} thành công, balance mới = {}",
          authenticatedUserId, amount, newBalance);
      return ServerResponse.reply("USER_DEPOSIT", requestId)
          .success(true)
          .message("Nạp tiền thành công")
          .data(data)
          .build();

    } catch (Exception e) {
      log.warn("USER_DEPOSIT thất bại userId={}: {}", authenticatedUserId, e.getMessage());
      return ServerResponse.replyError("USER_DEPOSIT", requestId,
          "Lỗi: " + e.getMessage());
    }
  }

  /**
   * USER_BIDS — lịch sử bid của user. Chưa có service.
   */
  public ServerResponse handleUserBids(JsonObject request, String requestId,
                                       int authenticatedUserId) {
    return ServerResponse.replyError("USER_BIDS", requestId,
        "Tính năng chưa được hỗ trợ");
  }
}
