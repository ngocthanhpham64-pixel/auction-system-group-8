package vn.edu.vnu.uet.group8.client.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.TransactionHistoryEntry;
import vn.edu.vnu.uet.group8.common.dto.model.UserBidHistoryDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserProfileDTO;
import vn.edu.vnu.uet.group8.common.dto.response.LoginResponse;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * Service quản lý thông tin người dùng: profile, ví tiền, lịch sử đấu giá.
 * -Tất cả callback chạy trên FX Thread - controller có thể update UI trực tiếp
 * -Service không hiển thị dialog/alert - controller tự quyết định
 */
public final class UserService{
  //Gson hỗ trợ Instant - bắt buộc vì Record có trường Instant placedAt
  private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

  private UserService(){}
  // Profile
  /**
   * Tải thông tin profile của user hiện tại( trả về LoginResponse)
   * Nếu thành công: cập nhật (ClientModel.setCurrentUser(LoginResponse) và SessionManager phòng khi thông tin thay đổi
   * Nếu thất bại: callback nhận null
   * @param onResult callback nhận (UserProfileDTO) (null nếu lỗi)
   */
  public static void loadProfile(Consumer<UserProfileDTO> onResult){
    AuctionClient.getInstance().sendAuthenticatedRequest(ActionType.USER_PROFILE,null,
            response -> {
        if(response.isSuccess()){
            UserProfileDTO user = GsonUtil.toObject(response.getData(), UserProfileDTO.class);
            if(user != null){
                // Cập nhật lại LoginResponse trong ClientModel để giữ nguyên Token
                LoginResponse updatedLogin = LoginResponse.success(
                    user.getUserId(), user.getUsername(), user.getFullName(), 
                    user.getEmail(), SessionManager.getRole(), SessionManager.getAuthToken()
                );
                ClientModel.getInstance().setCurrentUser(updatedLogin);
                SessionManager.setSession(SessionManager.getAuthToken(),
                        user.getUserId(),
                        user.getUsername(),
                        user.getFullName(),
                        SessionManager.getRole());
                LOGGER.fine("Profile loaded: " + user.getUsername());
            } else{
                LOGGER.warning("loadProfile received null data despite success response");
            }
            if(onResult != null) onResult.accept(user);
        } else{
            String err = response.getMessage() != null ? response.getMessage() : "Unknown error";
            LOGGER.log(Level.WARNING,"loadProfile failed: {0}" , err);
            if(onResult != null) onResult.accept(null);
        }
    });
  }

  /**
   * Rút tiền từ tài khoản.
   * Sau khi thành công, cập nhật số dư mới (cần gọi loadProfile hoặc trừ ở ClientModel).
   * @param amount số tiền rút (phải>0)
   * @param onResult callback nhận true nếu thành công, false nếu thất bại
   */
  public static void withdraw(BigDecimal amount, Consumer<Boolean> onResult) {
    if(amount == null || amount.compareTo(BigDecimal.ZERO)<=0){
        LOGGER.warning("Withdraw rejected: invalid amount");
        if(onResult != null) onResult.accept(false);
        return;
    }
    if(!SessionManager.isLoggedIn()){
        LOGGER.warning("withdraw called while not logged in");
        if(onResult != null) onResult.accept(false);
        return;
    }

    Map<String, String> payload = new HashMap<>();
    payload.put("amount", amount.toPlainString());
    payload.put("transactionId", java.util.UUID.randomUUID().toString());
    payload.put("paymentMethod", "BANK_TRANSFER");

    AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.USER_WITHDRAW, payload,
            response -> {
                if (response.isSuccess()) {
                    BigDecimal newBalance = GsonUtil.toObject(response.getData(), BigDecimal.class);
                    if (newBalance != null) {
                        ClientModel.getInstance().updateBalance(newBalance);
                    } else {
                        // Fallback an toàn nếu parse lỗi
                        ClientModel.getInstance().updateBalance(ClientModel.getInstance().getBalance().subtract(amount));
                    }
                    LOGGER.info("Withdraw successful, new balance: " + newBalance);
                    if (onResult != null) onResult.accept(true);
                } else {
                    String err = response.getMessage() != null ? response.getMessage() : "Rút tiền thất bại";
                    LOGGER.log(Level.WARNING, "withdraw failed: {0}", err);
                    if (onResult != null) onResult.accept(false);
                }
            });
  }
  
  /**
   * Nạp tiền vào tài khoản.
   * Sau khi thành công, cập nhật số dư mới vào(ClientModel)
   * @param amount số tiền nạp(phải>0)
   * @param onResult callback nhận true nếu thành công, false nếu thất bại
   */
  public static void deposit(BigDecimal amount, Consumer<Boolean> onResult){
    if(amount == null || amount.compareTo(BigDecimal.ZERO)<=0){
        LOGGER.warning("Deposit rejected: invalid amount");
        if(onResult != null) onResult.accept(false);
        return;
    }
    if(! SessionManager.isLoggedIn()){
        LOGGER.warning("deposit called while not logged in");
        if(onResult != null) onResult.accept(false);
        return;
    }
    Map<String, String> payload = new HashMap<>();
    payload.put("amount", amount.toPlainString());
    payload.put("transactionId", java.util.UUID.randomUUID().toString());
    payload.put("paymentMethod", "BANK_TRANSFER");

    AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.USER_DEPOSIT, payload,
            response -> {
                if (response.isSuccess()) {
                    //Server trả về newBalance trong data
                    BigDecimal newBalance = GsonUtil.toObject(response.getData(), BigDecimal.class);
                    if (newBalance != null) {
                        ClientModel.getInstance().updateBalance(newBalance);
                        LOGGER.info("Deposit successful, new balance: " + newBalance);
                    } else {
                        LOGGER.warning("Deposit successful but newBalance is null");
                    }
                    if (onResult != null) onResult.accept(true);
                } else {
                    String err = response.getMessage() != null ? response.getMessage() : "Nạp tiền thất bại";
                    LOGGER.log(Level.WARNING, "deposit failed: {0}", err);
                    if (onResult != null) onResult.accept(false);
                }
            });
  }

  /**
   * Lấy danh sách lịch sử đấu giá của user hiện tại
   */
  public static void loadMyBids(Consumer<List<UserBidHistoryDTO>> onResult) {
    AuctionClient.getInstance()
        .sendAuthenticatedRequest(
            ActionType.USER_BIDS,
            null,
            response -> {
              List<UserBidHistoryDTO> bids =
                  GsonUtil.toList(response.getData(), UserBidHistoryDTO.class);
              if (bids == null) {
                LOGGER.warning("loadMyBids returned null data");
                bids = List.of();
              }
              if (onResult != null) onResult.accept(bids);
            });
  }

  /**
   * Đổi mật khẩu.
   */
  public static void changePassword(String oldPass, String newPass, Consumer<Boolean> onResult) {
    Map<String, String> payload = Map.of("oldPassword", oldPass, "newPassword", newPass);
    AuctionClient.getInstance().sendAuthenticatedRequest(
        ActionType.USER_CHANGE_PASSWORD, payload,
        response -> {
            if (!response.isSuccess()) {
                LOGGER.warning("Đổi mật khẩu thất bại: " + response.getMessage());
            }
            if (onResult != null) onResult.accept(response.isSuccess());
        }
    );
  }

  /**
   * Tải lịch sử ví.
   */
  public static void loadTransactions(Consumer<List<TransactionHistoryEntry>> onResult) {
    AuctionClient.getInstance().sendAuthenticatedRequest(
        ActionType.WALLET_GET_TRANSACTIONS, null,
        response -> {
            if (response.isSuccess()) {
                List<TransactionHistoryEntry> list = GsonUtil.toList(response.getData(), TransactionHistoryEntry.class);
                if (onResult != null) onResult.accept(list != null ? list : List.of());
            } else {
                if (onResult != null) onResult.accept(List.of());
            }
        }
    );
  }

  /**
   * Cập nhật thông tin cá nhân.
   */
  public static void updateProfile(String fullname, String phone, String address, String avatarBase64, Consumer<Boolean> onResult) {
      Map<String, String> payload = new HashMap<>();
      payload.put("fullname", fullname);
      payload.put("phone", phone);
      if (address != null && !address.isBlank()) payload.put("address", address);
      if (avatarBase64 != null && !avatarBase64.isBlank()) payload.put("avatarUrl", avatarBase64);

      AuctionClient.getInstance().sendAuthenticatedRequest(
          ActionType.USER_UPDATE_PROFILE, payload,
          response -> {
              if (!response.isSuccess()) {
                  LOGGER.warning("Cập nhật hồ sơ thất bại: " + response.getMessage());
              }
              if (onResult != null) onResult.accept(response.isSuccess());
          }
      );
  }
}