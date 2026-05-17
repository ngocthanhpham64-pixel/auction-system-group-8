package vn.edu.vnu.uet.group8.client.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javafx.application.Platform;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.common.dto.model.AdminStatsDTO;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.UserAdminDTO;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * Service xử lý các chức năng Admin.
 * 
 * Đã kết nối với Backend thực tế thông qua các ActionType Admin.
 */
public final class AdminService {

    private static final Logger LOGGER = Logger.getLogger(AdminService.class.getName());

    /** Set status hợp lệ cho user. */
    private static final List<String> VALID_USER_STATUSES = List.of(
            UserAdminDTO.STATUS_ACTIVE,
            UserAdminDTO.STATUS_SUSPENDED,
            UserAdminDTO.STATUS_BANNED
    );

    /** Utility class — không cho khởi tạo. */
    private AdminService() {}

    // ========================================
    // DASHBOARD
    // ========================================

    /**
     * Lấy số liệu dashboard tổng quan từ server.
     */
    public static void getStats(Consumer<AdminStatsDTO> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        LOGGER.fine("ADMIN_DASHBOARD: getStats() requested");

        AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.ADMIN_DASHBOARD, null,
            resp -> {
                if (resp.isSuccess()) {
                    deliver(callback, resp.getData(AdminStatsDTO.class));
                } else {
                    LOGGER.warning("ADMIN_DASHBOARD failed: " + resp.getMessage());
                    deliver(callback, AdminStatsDTO.empty());
                }
            });
    }

    // ========================================
    // USERS
    // ========================================

    /**
     * Lấy danh sách user từ server.
     */
    public static void getUsers(Consumer<List<UserAdminDTO>> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        LOGGER.fine("ADMIN_GET_USERS: getUsers() requested");

        AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.ADMIN_GET_USERS, null,
            resp -> {
                if (resp.isSuccess()) {
                    List<UserAdminDTO> users = GsonUtil.toList(resp.getData(), UserAdminDTO.class);
                    deliver(callback, users != null ? users : List.of());
                } else {
                    LOGGER.warning("ADMIN_GET_USERS failed: " + resp.getMessage());
                    deliver(callback, List.of());
                }
            });
    }

    /**
     * Cập nhật trạng thái user (ACTIVE / SUSPENDED / BANNED).
     */
    public static void updateUserStatus(int userId, String newStatus, Consumer<Boolean> callback) {
        Objects.requireNonNull(callback, "callback must not be null");

        // Validate input
        if (userId <= 0) {
            LOGGER.warning("updateUserStatus: invalid userId=" + userId);
            deliver(callback, false);
            return;
        }
        if (!VALID_USER_STATUSES.contains(newStatus)) {
            LOGGER.warning(() -> "updateUserStatus: invalid status=" + newStatus
                    + " (valid: " + VALID_USER_STATUSES + ")");
            deliver(callback, false);
            return;
        }

        LOGGER.info(() -> "ADMIN_UPDATE_USER_STATUS: userId=" + userId + " → " + newStatus);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("status", newStatus);

        AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.ADMIN_UPDATE_USER_STATUS, payload,
            resp -> deliver(callback, resp.isSuccess()));
    }

    // ========================================
    // AUCTIONS
    // ========================================

    /**
     * Lấy danh sách phiên đấu giá từ Server.
     */
    public static void getAuctions(Consumer<List<AuctionItemDTO>> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        LOGGER.fine("ADMIN_GET_AUCTIONS: getAuctions() requested");

        AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.ADMIN_GET_AUCTIONS, null,
            resp -> {
                if (resp.isSuccess()) {
                    List<AuctionItemDTO> list = GsonUtil.toList(resp.getData(), AuctionItemDTO.class);
                    deliver(callback, list != null ? list : List.of());
                } else {
                    LOGGER.warning("ADMIN_GET_AUCTIONS failed: " + resp.getMessage());
                    deliver(callback, List.of());
                }
            });
    }

    /**
     * Hủy phiên đấu giá phía Server.
     */
    public static void cancelAuction(int sessionId, Consumer<Boolean> callback) {
        Objects.requireNonNull(callback, "callback must not be null");

        if (sessionId <= 0) {
            LOGGER.warning("cancelAuction: invalid sessionId=" + sessionId);
            deliver(callback, false);
            return;
        }

        LOGGER.info(() -> "ADMIN_CANCEL_AUCTION: sessionId=" + sessionId);

        AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.ADMIN_CANCEL_AUCTION, sessionId,
            resp -> deliver(callback, resp.isSuccess()));
    }

    // ========================================
    // PRIVATE HELPERS
    // ========================================

    /**
     * Gọi callback an toàn trên FX Thread.
     * Tránh IllegalStateException khi update UI từ background thread.
     */
    private static <T> void deliver(Consumer<T> callback, T value) {
        if (Platform.isFxApplicationThread()) {
            callback.accept(value);
        } else {
            Platform.runLater(() -> callback.accept(value));
        }
    }
}