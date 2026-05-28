package vn.edu.vnu.uet.group8.client.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.networking.ResponseDispatcher;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.enums.EventType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * Service quản lý thông báo (notification).
 *
 * <h3>Trách nhiệm:</h3>
 * <ul>
 *   <li>Load list notification</li>
 *   <li>Đánh dấu đã đọc (mark read)</li>
 *   <li>Subscribe/unsubscribe broadcast NOTIFICATION (push real-time)</li>
 *   <li>Cập nhật {@link ClientModel} - badge tự update qua property binding</li>
 * </ul>
 *
 * <h3>Auth:</h3>
 * Mọi method (trừ unsubscribe) đều yêu cầu login. Nếu chưa login, callback
 * onFailure sẽ được gọi với message "Bạn cần đăng nhập".
 *
 * <h3>Cải thiện so với bản cũ:</h3>
 * <ul>
 *   <li><b>Logger</b> ghi rõ flow</li>
 *   <li><b>Helper {@code reportFailure}</b> - gom 4 chỗ lặp lại pattern null check</li>
 *   <li><b>Class-level Javadoc</b></li>
 *   <li><b>Empty list fallback</b> - khi parse null vẫn deliver empty list</li>
 *   <li><b>Const ERR_NOT_LOGGED_IN</b> - thay vì hardcode string khắp nơi</li>
 * </ul>
 */
public final class NotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());

    private static final String ERR_NOT_LOGGED_IN = "Bạn cần đăng nhập";

    /** Utility class — không cho khởi tạo. */
    private NotificationService() {}

    // ========================================
    // LOAD
    // ========================================

    /**
     * Tải toàn bộ notification của user hiện tại.
     *
     * @param onSuccess nhận list (không bao giờ null - có thể empty)
     * @param onFailure nhận message lỗi
     */
    public static void loadAll(Consumer<List<NotificationDTO>> onSuccess,
                               Consumer<String> onFailure) {
        if (!SessionManager.isLoggedIn()) {
            reportFailure(onFailure, ERR_NOT_LOGGED_IN);
            return;
        }

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.NOTIF_GET_ALL, null,
                response -> {
                    if (response.isSuccess()) {
                        List<NotificationDTO> parsed = GsonUtil.toList(response.getData(), NotificationDTO.class);
                        final List<NotificationDTO> list = parsed != null ? parsed : Collections.emptyList();
                        ClientModel.getInstance().setNotifications(list);
                        LOGGER.fine(() -> "Loaded " + list.size() + " notifications");
                        if (onSuccess != null) onSuccess.accept(list);
                    } else {
                        LOGGER.warning("NOTIF_GET_ALL failed: " + response.getMessage());
                        reportFailure(onFailure, response.getMessage());
                    }
                });
    }

    // ========================================
    // MARK READ
    // ========================================

    /**
     * Đánh dấu 1 notification là đã đọc.
     * Sau khi thành công, ClientModel tự giảm unread count → badge UI giảm theo.
     *
     * @param notificationId ID notification
     * @param onSuccess callback khi thành công (null bỏ qua)
     * @param onFailure callback khi lỗi (null bỏ qua)
     */
    public static void markRead(int notificationId,
                                Runnable onSuccess,
                                Consumer<String> onFailure) {
        if (!SessionManager.isLoggedIn()) {
            reportFailure(onFailure, ERR_NOT_LOGGED_IN);
            return;
        }

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.NOTIF_MARK_READ, Map.of("notificationId", notificationId),
                response -> {
                    if (response.isSuccess()) {
                        ClientModel.getInstance().markNotificationRead(notificationId);
                        LOGGER.fine(() -> "Marked notification " + notificationId + " as read");
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        LOGGER.warning(() -> "NOTIF_MARK_READ failed for " + notificationId
                                + ": " + response.getMessage());
                        reportFailure(onFailure, response.getMessage());
                    }
                });
    }

    // ========================================
    // DELETE
    // ========================================

    /**
     * Xóa 1 notification khỏi cơ sở dữ liệu và ClientModel.
     *
     * @param notificationId ID notification cần xóa
     * @param onSuccess callback khi thành công
     * @param onFailure callback khi lỗi
     */
    public static void deleteNotification(int notificationId,
                                          Runnable onSuccess,
                                          Consumer<String> onFailure) {
        if (!SessionManager.isLoggedIn()) {
            reportFailure(onFailure, ERR_NOT_LOGGED_IN);
            return;
        }
        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.NOTIF_DELETE, Map.of("notificationId", notificationId),
                response -> {
                    if (response.isSuccess()) {
                        // Tự động xóa nội bộ trong ClientModel để UI cập nhật ngay
                        ClientModel.getInstance().getNotifications().removeIf(n -> n.getId() == notificationId);
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        reportFailure(onFailure, response.getMessage());
                    }
                });
    }

    // ========================================
    // REAL-TIME PUSH
    // ========================================

    /**
     * Đăng ký nhận thông báo push real-time.
     * <p>Pattern giống {@link AuctionService#subscribeAuctionStatus(Consumer)}.
     * <p>Tự động parse {@link NotificationDTO}, thêm vào {@link ClientModel}
     * (badge tự tăng qua binding), sau đó gọi listener với object đã parse.
     *
     * @param onNew listener nhận {@link NotificationDTO} (chạy trên FX Thread)
     * @return wrapper dùng để hủy đăng ký
     */
    public static Consumer<ServerResponse> subscribePush(Consumer<NotificationDTO> onNew) {
        if (onNew == null) {
            LOGGER.warning("subscribePush: listener null");
            return null;
        }
        Consumer<ServerResponse> wrapper = response -> {
            NotificationDTO notif = GsonUtil.toObject(response.getData(), NotificationDTO.class);
            if (notif == null) {
                LOGGER.fine("NOTIFICATION broadcast: notif null, skip");
                return;
            }
            ClientModel.getInstance().addNotification(notif);
            onNew.accept(notif);
        };
        ResponseDispatcher.subscribe(EventType.NOTIFICATION, wrapper);
        LOGGER.fine("Subscribed NOTIFICATION push");
        return wrapper;
    }

    /**
     * Hủy đăng ký nhận thông báo push.
     * Phải truyền đúng wrapper trả về từ {@link #subscribePush}.
     */
    public static void unsubscribePush(Consumer<ServerResponse> wrapper) {
        if (wrapper == null) return;
        ResponseDispatcher.unsubscribe(EventType.NOTIFICATION, wrapper);
        LOGGER.fine("Unsubscribed NOTIFICATION push");
    }

    // ========================================
    // PRIVATE HELPER
    // ========================================

    /** Gom logic null-check + accept message lỗi vào 1 chỗ. */
    private static void reportFailure(Consumer<String> onFailure, String message) {
        if (onFailure != null) {
            onFailure.accept(message != null ? message : "Lỗi không xác định");
        }
    }
}