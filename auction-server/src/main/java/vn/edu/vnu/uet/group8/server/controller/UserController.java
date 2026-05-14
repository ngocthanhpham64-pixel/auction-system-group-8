package vn.edu.vnu.uet.group8.server.controller;

import java.math.BigDecimal;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.PaymentMethod;
import vn.edu.vnu.uet.group8.server.network.RequestParser;
import vn.edu.vnu.uet.group8.server.service.user.BalanceService;
import vn.edu.vnu.uet.group8.server.service.user.PasswordService;
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
  private final PasswordService passwordService;

  public UserController(ProfileService profileService,
                        BalanceService balanceService,
                        PasswordService passwordService) {
    this.profileService = profileService;
    this.balanceService = balanceService;
    this.passwordService = passwordService;
  }

  // ═══════════════════════════════════════════════════
  // Truy vấn
  // ═══════════════════════════════════════════════════
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


  // ═══════════════════════════════════════════════════
  // Quản lý tài khoản
  // ═══════════════════════════════════════════════════
  // Đổi mật khẩu
  public ServerResponse handleChangePassword(JsonObject request, String requestId, int authenticatedUserId) {
    try {
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
          ? request.getAsJsonObject("payload") : new JsonObject();
      
      String oldPassword = RequestParser.requireString(payload, "oldPassword");
      String newPassword = RequestParser.requireString(payload, "newPassword");

      log.info("User {} đổi mật khẩu", authenticatedUserId);

      passwordService.changePassword(authenticatedUserId, oldPassword, newPassword);

      return ServerResponse.reply("CHANGE_PASSWORD", requestId)
          .success(true)
          .message("Đổi mật khẩu thành công")
          .build();
    } catch (SQLException e) {
      log.error("Không thể đổi mật khẩu");
      return ServerResponse.replyError("CHANGE_PASSWORD", requestId, "Lỗi: " + e.getMessage());
    } catch (Exception e) {
      log.error("Lỗi khi đổi mật khẩu userId={}: {}", authenticatedUserId, e.getMessage());
      return ServerResponse.replyError("CHANGE_PASSWORD", requestId, "Lỗi: " + e.getMessage());
    }
  }

  // Rút tiền
  public ServerResponse handleWithdraw(JsonObject request, String requestId, int authenticatedUserId) {
    try {
      JsonObject payload = request.has("payload")
          && request.get("payload").isJsonObject()
          ? request.getAsJsonObject("payload") : new JsonObject();

      String amountStr = RequestParser.requireString(payload, "amount");
      String transactionId = RequestParser.requireString(payload, "transactionId");
      String paymentMethodStr = RequestParser.requireString(payload, "paymentMethod");

      BigDecimal amount = new BigDecimal(amountStr);
      PaymentMethod method = PaymentMethod.valueOf(paymentMethodStr);

      BigDecimal newBalance = balanceService.withdrawBalance(
          authenticatedUserId, amount, transactionId, method);

      // JsonObject data = new JsonObject();
      // data.addProperty("newBalance", newBalance.toPlainString());

      log.info("User {} rút {} thành công, balance mới = {}",
          authenticatedUserId, amount, newBalance);
      return ServerResponse.reply("USER_WITHDRAW", requestId)
          .success(true)
          .message("Rút tiền thành công")
          // .data(data)
          .build();

    } catch (Exception e) {
      log.warn("USER_WITHDRAW thất bại userId={}: {}", authenticatedUserId, e.getMessage());
      return ServerResponse.replyError("USER_WITHDRAW", requestId,
          "Lỗi: " + e.getMessage());
    }
  }

  // Lịch sử ví tiền
  public ServerResponse handleGetTransactions(JsonObject request, String requestId, int authenticatedUserId) {
    try {
      var transactions = balanceService.getTransactionRecordByUserId(authenticatedUserId);
      return ServerResponse.reply("USER_TRANSACTIONS", requestId)
          .success(true)
          .message("Lấy lịch sử giao dịch thành công")
          .data(transactions)
          .build();
    } catch (Exception e) {
      log.error("Lỗi lấy lịch sử giao dịch userId={}: {}", authenticatedUserId, e.getMessage());
      return ServerResponse.replyError("USER_TRANSACTIONS", requestId, "Lỗi: " + e.getMessage());
    }
  }

  // ═══════════════════════════════════════════════════
  // Dành cho Admin
  // ═══════════════════════════════════════════════════
}
