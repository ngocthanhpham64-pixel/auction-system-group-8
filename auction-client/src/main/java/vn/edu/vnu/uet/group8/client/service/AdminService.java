package vn.edu.vnu.uet.group8.client.service;

import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.common.dto.*;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * Service xử lý các chức năng Admin.
 */
public class AdminService {
    // MOCK DATA - XÓA KHI BACKEND SẴN SÀNG
    private static final List<UserAdminDTO> mockUsers = new ArrayList<>();
    private static final AdminStatsDTO mockStats;
    private static final List<AuctionSummaryDTO> mockAuctions = new ArrayList<>();

    static {
        mockUsers.add(new UserAdminDTO(1, "alice", "alice@example.com", "MEMBER", "ACTIVE"));
        mockUsers.add(new UserAdminDTO(2, "bob", "bob@example.com", "MEMBER", "ACTIVE"));
        mockUsers.add(new UserAdminDTO(3, "charlie", "charlie@example.com", "SELLER", "SUSPENDED"));
        mockUsers.add(new UserAdminDTO(4, "diana", "diana@example.com", "ADMIN", "ACTIVE"));
        mockUsers.add(new UserAdminDTO(5, "eve", "eve@example.com", "MEMBER", "BANNED"));

        mockStats = new AdminStatsDTO(128, 1450, new BigDecimal("985000000"), 234);

        mockAuctions.add(new AuctionSummaryDTO(101, "Rolex Submariner", new BigDecimal("850000000"), "ACTIVE", Instant.now().plusSeconds(3600)));
        mockAuctions.add(new AuctionSummaryDTO(102, "Patek Philippe Nautilus", new BigDecimal("1860000000"), "ACTIVE", Instant.now().plusSeconds(1800)));
        mockAuctions.add(new AuctionSummaryDTO(103, "Mercedes 1960", new BigDecimal("500000000"), "ENDED_NO_BID", Instant.now().minusSeconds(86400)));
        mockAuctions.add(new AuctionSummaryDTO(104, "Van Gogh Painting", new BigDecimal("2500000000"), "SOLD", Instant.now().minusSeconds(172800)));
    }
    // ========= DASHBOARD =========
    public static void getStats(Consumer<AdminStatsDTO> callback){
        // MOCK
        callback.accept(mockStats);
        // REAL (bỏ comment khi có backend)
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_DASHBOARD, null,
        //     resp -> callback.accept(GsonUtil.toObject(resp.getData(), AdminStatsDTO.class)));
    }
    // ========= USERS ============
    public static void getUsers(Consumer<List<UserAdminDTO>> callback){
        // MOCK
        callback.accept(new ArrayList<>(mockUsers));
        // REAL
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_GET_USERS, null,
        //     resp -> callback.accept(GsonUtil.toList(resp.getData(), UserAdminDTO.class)));
    }
    public static void updateUserStatus(int userId, String newStatus, Consumer<Boolean> callback){
        // MOCK
        mockUsers.stream().filter(u->u.getId() == userId).findFirst()
                .ifPresent(u-> u.setStatus(newStatus));
        callback.accept(true);
        // REAL
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_UPDATE_USER_STATUS,
        //     new UserStatusUpdateRequest(userId, newStatus),
        //     resp -> callback.accept(resp.isSuccess()));
    }
    // ========== AUCTIONS =============
    public static void getAuctions(Consumer<List<AuctionSummaryDTO>> callback){
        // MOCK
        callback.accept(new ArrayList<>(mockAuctions));
        // REAL
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_GET_AUCTIONS, null,
        //     resp -> callback.accept(GsonUtil.toList(resp.getData(), AuctionSummaryDTO.class)));
    }
    public static void cancelAuction(int auctionId, Consumer<Boolean> callback) {
        // MOCK
        mockAuctions.stream().filter(a -> a.getId() == auctionId).findFirst()
                .ifPresent(a -> a.setStatus("CANCELLED"));
        callback.accept(true);
        // REAL
        // AuctionClient.getInstance().sendAuthenticatedRequest(
        //     ActionType.ADMIN_CANCEL_AUCTION, auctionId,
        //     resp -> callback.accept(resp.isSuccess()));
    }
}
