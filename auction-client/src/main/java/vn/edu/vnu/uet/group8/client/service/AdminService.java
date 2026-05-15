package vn.edu.vnu.uet.group8.client.service;

import javafx.application.Platform;
import vn.edu.vnu.uet.group8.common.dto.AdminStatsDTO;
import vn.edu.vnu.uet.group8.common.dto.AuctionSummaryDTO;
import vn.edu.vnu.uet.group8.common.dto.UserAdminDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service xử lý các chức năng Admin.
 *
 * <h3>Trạng thái hiện tại: MOCK</h3>
 * Service đang dùng mock data trong khi backend hoàn thiện 5 ActionType admin.
 * Khi backend sẵn sàng, bỏ comment phần REAL và xóa phần MOCK ở mỗi method.
 *
 * <h3>Cải thiện so với bản trước:</h3>
 * <ul>
 *   <li><b>Logger</b> - debug rõ ràng, không silent fail</li>
 *   <li><b>Simulate async</b> (200ms latency) - khớp với hành vi network thật</li>
 *   <li><b>Callback luôn trên FX Thread</b> - UI update an toàn</li>
 *   <li><b>Defensive copy</b> mock list - tránh client mutation ảnh hưởng mock</li>
 *   <li><b>Input validation</b> - userId/auctionId/status phải hợp lệ</li>
 *   <li><b>Error path đúng</b> - userId không tồn tại → trả false (không phải true)</li>
 *   <li><b>Utility class</b> - private constructor, all static</li>
 * </ul>
 *
 * <h3>5 ActionType admin tương ứng BE:</h3>
 * <ul>
 *   <li>ADMIN_DASHBOARD - {@link #getStats(Consumer)}</li>
 *   <li>ADMIN_GET_USERS - {@link #getUsers(Consumer)}</li>
 *   <li>ADMIN_UPDATE_USER_STATUS - {@link #updateUserStatus(int, String, Consumer)}</li>
 *   <li>ADMIN_GET_AUCTIONS - {@link #getAuctions(Consumer)}</li>
 *   <li>ADMIN_CANCEL_AUCTION - {@link #cancelAuction(int, Consumer)}</li>
 * </ul>
 */
public final class AdminService {

    private static final Logger LOGGER = Logger.getLogger(AdminService.class.getName());

    /** Độ trễ giả lập (ms) để mô phỏng network latency thật. */
    private static final long MOCK_LATENCY_MS = 200;

    /** Set status hợp lệ cho user. */
    private static final List<String> VALID_USER_STATUSES = List.of(
            UserAdminDTO.STATUS_ACTIVE,
            UserAdminDTO.STATUS_SUSPENDED,
            UserAdminDTO.STATUS_BANNED
    );

    // ========================================
    // MOCK DATA — XÓA KHI BACKEND SẴN SÀNG
    // ========================================

    private static final List<UserAdminDTO> mockUsers = new ArrayList<>();
    private static final AdminStatsDTO mockStats;
    private static final List<AuctionSummaryDTO> mockAuctions = new ArrayList<>();

    static {
        // Mock users — đa dạng role + status
        mockUsers.add(new UserAdminDTO(1, "alice",   "alice@example.com",   "MEMBER",    "ACTIVE"));
        mockUsers.add(new UserAdminDTO(2, "bob",     "bob@example.com",     "MEMBER",    "ACTIVE"));
        mockUsers.add(new UserAdminDTO(3, "charlie", "charlie@example.com", "SELLER",    "SUSPENDED"));
        mockUsers.add(new UserAdminDTO(4, "diana",   "diana@example.com",   "ADMIN",     "ACTIVE"));
        mockUsers.add(new UserAdminDTO(5, "eve",     "eve@example.com",     "MEMBER",    "BANNED"));

        // Mock stats — số liệu hợp lý cho demo
        mockStats = new AdminStatsDTO(128, 1450, new BigDecimal("985000000"), 234);

        // Mock auctions — đa dạng status
        mockAuctions.add(new AuctionSummaryDTO(101, "Rolex Submariner",        new BigDecimal("850000000"),  "ACTIVE",       Instant.now().plusSeconds(3600)));
        mockAuctions.add(new AuctionSummaryDTO(102, "Patek Philippe Nautilus", new BigDecimal("1860000000"), "ACTIVE",       Instant.now().plusSeconds(1800)));
        mockAuctions.add(new AuctionSummaryDTO(103, "Mercedes 1960",           new BigDecimal("500000000"),  "ENDED_NO_BID", Instant.now().minusSeconds(86400)));
        mockAuctions.add(new AuctionSummaryDTO(104, "Van Gogh Painting",       new BigDecimal("2500000000"), "SOLD",         Instant.now().minusSeconds(172800)));
    }

    /** Utility class — không cho khởi tạo. */
    private AdminService() {}

    // ========================================
    // DASHBOARD
    // ========================================

    /**
     * Lấy số liệu dashboard tổng quan.
     * Callback luôn được gọi trên FX Thread sau ~200ms (mô phỏng network).
     *
     * @param callback nhận AdminStatsDTO (không bao giờ null)
     */
    public static void getStats(Consumer<AdminStatsDTO> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        LOGGER.fine("ADMIN_DASHBOARD: getStats() requested");

        runMockAsync(() -> deliver(callback, mockStats));

        // REAL (bỏ comment khi có backend):
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_DASHBOARD, null,
        //     resp -> {
        //         if (resp.isSuccess()) {
        //             deliver(callback, resp.getData(AdminStatsDTO.class));
        //         } else {
        //             LOGGER.warning("ADMIN_DASHBOARD failed: " + resp.getMessage());
        //             deliver(callback, AdminStatsDTO.empty());
        //         }
        //     });
    }

    // ========================================
    // USERS
    // ========================================

    /**
     * Lấy danh sách user.
     * Callback nhận defensive copy của mock list để tránh mutation ngoài ý muốn.
     */
    public static void getUsers(Consumer<List<UserAdminDTO>> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        LOGGER.fine("ADMIN_GET_USERS: getUsers() requested");

        runMockAsync(() -> deliver(callback, new ArrayList<>(mockUsers)));

        // REAL:
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_GET_USERS, null,
        //     resp -> {
        //         if (resp.isSuccess()) {
        //             List<UserAdminDTO> users = resp.getDataList(UserAdminDTO.class);
        //             deliver(callback, users != null ? users : List.of());
        //         } else {
        //             LOGGER.warning("ADMIN_GET_USERS failed: " + resp.getMessage());
        //             deliver(callback, List.of());
        //         }
        //     });
    }

    /**
     * Cập nhật trạng thái user (ACTIVE / SUSPENDED / BANNED).
     * <p>Trả về <code>true</code> nếu update thành công, <code>false</code> nếu:
     * <ul>
     *   <li>userId không tồn tại trong hệ thống</li>
     *   <li>newStatus không hợp lệ</li>
     *   <li>Server trả lỗi</li>
     * </ul>
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

        runMockAsync(() -> {
            Optional<UserAdminDTO> userOpt = mockUsers.stream()
                    .filter(u -> u.getId() == userId)
                    .findFirst();

            if (userOpt.isEmpty()) {
                LOGGER.warning(() -> "updateUserStatus: userId=" + userId + " not found");
                deliver(callback, false);
                return;
            }

            userOpt.get().setStatus(newStatus);
            LOGGER.fine(() -> "Updated user " + userId + " status → " + newStatus);
            deliver(callback, true);
        });

        // REAL:
        // Map<String, Object> payload = new HashMap<>();
        // payload.put("userId", userId);
        // payload.put("status", newStatus);
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_UPDATE_USER_STATUS, payload,
        //     resp -> deliver(callback, resp.isSuccess()));
    }

    // ========================================
    // AUCTIONS
    // ========================================

    /**
     * Lấy danh sách phiên đấu giá.
     */
    public static void getAuctions(Consumer<List<AuctionSummaryDTO>> callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        LOGGER.fine("ADMIN_GET_AUCTIONS: getAuctions() requested");

        runMockAsync(() -> deliver(callback, new ArrayList<>(mockAuctions)));

        // REAL:
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_GET_AUCTIONS, null,
        //     resp -> {
        //         if (resp.isSuccess()) {
        //             List<AuctionSummaryDTO> list = resp.getDataList(AuctionSummaryDTO.class);
        //             deliver(callback, list != null ? list : List.of());
        //         } else {
        //             LOGGER.warning("ADMIN_GET_AUCTIONS failed: " + resp.getMessage());
        //             deliver(callback, List.of());
        //         }
        //     });
    }

    /**
     * Hủy phiên đấu giá.
     * <p>Trả về <code>true</code> nếu hủy thành công, <code>false</code> nếu:
     * <ul>
     *   <li>auctionId không tồn tại</li>
     *   <li>Phiên không ở trạng thái ACTIVE (đã SOLD/CANCELLED/ENDED_NO_BID)</li>
     *   <li>Server trả lỗi</li>
     * </ul>
     */
    public static void cancelAuction(int auctionId, Consumer<Boolean> callback) {
        Objects.requireNonNull(callback, "callback must not be null");

        if (auctionId <= 0) {
            LOGGER.warning("cancelAuction: invalid auctionId=" + auctionId);
            deliver(callback, false);
            return;
        }

        LOGGER.info(() -> "ADMIN_CANCEL_AUCTION: auctionId=" + auctionId);

        runMockAsync(() -> {
            Optional<AuctionSummaryDTO> auctionOpt = mockAuctions.stream()
                    .filter(a -> a.getId() == auctionId)
                    .findFirst();

            if (auctionOpt.isEmpty()) {
                LOGGER.warning(() -> "cancelAuction: auctionId=" + auctionId + " not found");
                deliver(callback, false);
                return;
            }

            AuctionSummaryDTO auction = auctionOpt.get();
            if (!auction.isActive()) {
                LOGGER.warning(() -> "cancelAuction: auctionId=" + auctionId
                        + " cannot cancel (status=" + auction.getStatus() + ")");
                deliver(callback, false);
                return;
            }

            auction.setStatus(AuctionSummaryDTO.STATUS_CANCELLED);
            LOGGER.fine(() -> "Cancelled auction " + auctionId);
            deliver(callback, true);
        });

        // REAL:
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_CANCEL_AUCTION, auctionId,
        //     resp -> deliver(callback, resp.isSuccess()));
    }

    // ========================================
    // PRIVATE HELPERS
    // ========================================

    /**
     * Chạy mock task async để mô phỏng network latency.
     * Tránh block FX Thread khi mock — khớp hành vi với backend thật.
     */
    private static void runMockAsync(Runnable task) {
        CompletableFuture
                .runAsync(() -> {
                    try {
                        Thread.sleep(MOCK_LATENCY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    task.run();
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Mock task failed", ex);
                    return null;
                });
    }

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