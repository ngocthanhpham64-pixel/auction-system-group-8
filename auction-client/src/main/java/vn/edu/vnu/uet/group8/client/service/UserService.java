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

public final class UserService {

    private static final Logger LOGGER =
            Logger.getLogger(UserService.class.getName());

    private UserService() {}

    // =====================================================
    // PROFILE
    // =====================================================

    public static void loadProfile(Consumer<UserProfileDTO> onResult) {

        if (!AuctionClient.getInstance().isConnected()) {
            if (onResult != null) onResult.accept(null);
            return;
        }

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.USER_PROFILE,
                null,
                response -> {
                    if (response.isSuccess()) {

                        UserProfileDTO user =
                                GsonUtil.toObject(response.getData(),
                                        UserProfileDTO.class);

                        if (user != null) {

                            LoginResponse updatedLogin =
                                    LoginResponse.success(
                                            user.getUserId(),
                                            user.getUsername(),
                                            user.getFullName(),
                                            user.getEmail(),
                                            SessionManager.getRole(),
                                            SessionManager.getAuthToken()
                                    );

                            ClientModel.getInstance()
                                    .setCurrentUser(updatedLogin);

                            SessionManager.setSession(
                                    SessionManager.getAuthToken(),
                                    user.getUserId(),
                                    user.getUsername(),
                                    user.getFullName(),
                                    SessionManager.getRole()
                            );

                            LOGGER.fine("Profile loaded: "
                                    + user.getUsername());
                        }

                        if (onResult != null) onResult.accept(user);

                    } else {

                        LOGGER.warning("loadProfile failed: "
                                + response.getMessage());

                        if (onResult != null) onResult.accept(null);
                    }
                });
    }

    // =====================================================
    // WITHDRAW
    // =====================================================

    public static void withdraw(BigDecimal amount,
                                Consumer<Boolean> onResult) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            if (onResult != null) onResult.accept(false);
            return;
        }

        if (!SessionManager.isLoggedIn()) {

            if (onResult != null) onResult.accept(false);
            return;
        }

        if (!AuctionClient.getInstance().isConnected()) {

            if (onResult != null) onResult.accept(false);
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("amount", amount.toPlainString());
        payload.put("transactionId",
                java.util.UUID.randomUUID().toString());
        payload.put("paymentMethod", "BANK_TRANSFER");

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.USER_WITHDRAW,
                payload,
                response -> {

                    if (response.isSuccess()) {

                        BigDecimal newBalance =
                                GsonUtil.toObject(
                                        response.getData(),
                                        BigDecimal.class);

                        if (newBalance != null) {
                            ClientModel.getInstance()
                                    .updateBalance(newBalance);
                        }

                        if (onResult != null) onResult.accept(true);

                    } else {

                        LOGGER.warning("withdraw failed: "
                                + response.getMessage());

                        if (onResult != null) onResult.accept(false);
                    }
                });
    }

    // =====================================================
    // DEPOSIT
    // =====================================================

    public static void deposit(BigDecimal amount,
                               Consumer<Boolean> onResult) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            if (onResult != null) onResult.accept(false);
            return;
        }

        if (!SessionManager.isLoggedIn()) {

            if (onResult != null) onResult.accept(false);
            return;
        }

        if (!AuctionClient.getInstance().isConnected()) {

            if (onResult != null) onResult.accept(false);
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("amount", amount.toPlainString());
        payload.put("transactionId",
                java.util.UUID.randomUUID().toString());
        payload.put("paymentMethod", "BANK_TRANSFER");

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.USER_DEPOSIT,
                payload,
                response -> {

                    if (response.isSuccess()) {

                        BigDecimal newBalance =
                                GsonUtil.toObject(
                                        response.getData(),
                                        BigDecimal.class);

                        if (newBalance != null) {

                            ClientModel.getInstance()
                                    .updateBalance(newBalance);
                        }

                        if (onResult != null) onResult.accept(true);

                    } else {

                        LOGGER.warning("deposit failed: "
                                + response.getMessage());

                        if (onResult != null) onResult.accept(false);
                    }
                });
    }

    // =====================================================
    // LOAD MY BIDS
    // =====================================================

    public static void loadMyBids(
            Consumer<List<UserBidHistoryDTO>> onResult) {

        if (!AuctionClient.getInstance().isConnected()) {
            if (onResult != null) onResult.accept(List.of());
            return;
        }

        AuctionClient.getInstance()
                .sendAuthenticatedRequest(
                        ActionType.USER_BIDS,
                        null,
                        response -> {

                            List<UserBidHistoryDTO> bids =
                                    GsonUtil.toList(
                                            response.getData(),
                                            UserBidHistoryDTO.class);

                            if (bids == null) {
                                bids = List.of();
                            }

                            if (onResult != null) {
                                onResult.accept(bids);
                            }
                        });
    }

    // =====================================================
    // CHANGE PASSWORD
    // =====================================================

    public static void changePassword(String oldPass,
                                      String newPass,
                                      Consumer<Boolean> onResult) {

        // validate input trước
        if (oldPass == null || newPass == null
                || oldPass.isBlank() || newPass.isBlank()) {

            LOGGER.warning("changePassword invalid input");

            if (onResult != null) {
                onResult.accept(false);
            }
            return;
        }

        // check connection
        if (!AuctionClient.getInstance().isConnected()) {

            LOGGER.warning("changePassword called while disconnected");

            if (onResult != null) {
                onResult.accept(false);
            }
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("oldPassword", oldPass);
        payload.put("newPassword", newPass);

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.USER_CHANGE_PASSWORD,
                payload,
                response -> {

                    if (!response.isSuccess()) {

                        LOGGER.warning(
                                "Đổi mật khẩu thất bại: "
                                        + response.getMessage());
                    }

                    if (onResult != null) {
                        onResult.accept(response.isSuccess());
                    }
                });
    }

    // =====================================================
    // LOAD TRANSACTIONS
    // =====================================================

    public static void loadTransactions(
            Consumer<List<TransactionHistoryEntry>> onResult) {

        if (!AuctionClient.getInstance().isConnected()) {
            if (onResult != null) onResult.accept(List.of());
            return;
        }

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.WALLET_GET_TRANSACTIONS,
                null,
                response -> {

                    if (response.isSuccess()) {

                        List<TransactionHistoryEntry> list =
                                GsonUtil.toList(
                                        response.getData(),
                                        TransactionHistoryEntry.class);

                        if (onResult != null) {
                            onResult.accept(
                                    list != null ? list : List.of());
                        }

                    } else {

                        if (onResult != null) {
                            onResult.accept(List.of());
                        }
                    }
                });
    }

    // =====================================================
    // UPDATE PROFILE
    // =====================================================

    public static void updateProfile(
            String fullname,
            String phone,
            String address,
            String avatarBase64,
            Consumer<Boolean> onResult) {

        if (!AuctionClient.getInstance().isConnected()) {

            LOGGER.warning("updateProfile called while disconnected");

            if (onResult != null) {
                onResult.accept(false);
            }
            return;
        }

        Map<String, String> payload = new HashMap<>();

        // tránh null + trim input
        payload.put(
                "fullname",
                fullname != null ? fullname.trim() : ""
        );

        payload.put(
                "phone",
                phone != null ? phone.trim() : ""
        );

        if (address != null && !address.isBlank()) {
            payload.put("address", address.trim());
        }

        if (avatarBase64 != null
                && !avatarBase64.isBlank()) {

            payload.put("avatarUrl", avatarBase64);
        }

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.USER_UPDATE_PROFILE,
                payload,
                response -> {

                    if (!response.isSuccess()) {

                        LOGGER.warning(
                                "Cập nhật hồ sơ thất bại: "
                                        + response.getMessage());
                    }

                    if (onResult != null) {
                        onResult.accept(response.isSuccess());
                    }
                });
    }
}