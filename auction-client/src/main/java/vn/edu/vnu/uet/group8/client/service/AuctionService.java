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
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionStatusDTO;
import vn.edu.vnu.uet.group8.common.dto.model.CommentDTO;
import vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest;
import vn.edu.vnu.uet.group8.common.dto.request.ServerRequest;
import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.PriceUpdateBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.enums.EventType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * Service quản lý danh sách và trạng thái phiên đấu giá.
 *
 * <h3>Trách nhiệm:</h3>
 * <ul>
 *   <li>Gọi server lấy danh sách / chi tiết item</li>
 *   <li>Subscribe / unsubscribe broadcast real-time (PRICE_UPDATE, AUCTION_ENDED)</li>
 *   <li>Cập nhật {@link ClientModel} - listener UI tự rerender</li>
 * </ul>
 *
 * <h3>Pattern subscribe/unsubscribe:</h3>
 * <pre>{@code
 *   // Controller subscribe khi vào view
 *   subscription = AuctionService.subscribeAuctionStatus(this::handleStatusUpdate);
 *
 *   // Khi rời view, PHẢI unsubscribe để tránh memory leak
 *   AuctionService.unsubscribeAuctionStatus(subscription);
 * }</pre>
 *
 * <h3>Cải thiện so với bản cũ:</h3>
 * <ul>
 *   <li><b>Logger</b> thay cho silent fail</li>
 *   <li><b>Xóa dòng code thừa</b> {@code String json = GsonUtil.GSON.toJson(...)} không dùng</li>
 *   <li><b>Import explicit</b> thay vì wildcard {@code dto.*}</li>
 *   <li><b>Error path</b> - khi response fail, set ClientModel = empty list (không để stale data)</li>
 *   <li><b>Null check listener</b> - tránh NPE</li>
 *   <li><b>Javadoc đầy đủ</b> cho mọi public method</li>
 * </ul>
 */
public final class AuctionService {

    private static final Logger LOGGER = Logger.getLogger(AuctionService.class.getName());

    /** Utility class — không cho khởi tạo. */
    private AuctionService() {}

    // ========================================
    // LOAD DATA
    // ========================================

    /**
     * Tải toàn bộ item đang đấu giá từ server và lưu vào {@link ClientModel}.
     *
     * @param filter bộ lọc (category, keyword...); {@code null} để lấy tất cả
     * @param onDone callback gọi khi hoàn thành (thành công lẫn thất bại); {@code null} bỏ qua
     */
    public static void loadAll(GetAuctionsRequest filter, Runnable onDone) {
        if (!AuctionClient.getInstance().isConnected()) {
            LOGGER.warning("loadAll: client not connected — keeping existing items (demo mode safe)");
            // KHÔNG wipe ClientModel khi offline: cho phép demo mode hiển thị dữ liệu seed.
            runIfNotNull(onDone);
            return;
        }

        ServerRequest<GetAuctionsRequest> request = ServerRequest
                .<GetAuctionsRequest>builder(ActionType.ITEM_GET_ALL)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(filter)
                .build();

        AuctionClient.getInstance().sendRequest(request, response -> {
            if (response.isSuccess()) {
                List<AuctionItemDTO> parsed = GsonUtil.toList(response.getData(), AuctionItemDTO.class);
                final List<AuctionItemDTO> items = parsed != null ? parsed : Collections.emptyList();
                ClientModel.getInstance().setAuctionItems(items);
                LOGGER.fine(() -> "Loaded " + items.size() + " items");
            } else {
                LOGGER.warning("ITEM_GET_ALL failed: " + response.getMessage());
                ClientModel.getInstance().setAuctionItems(Collections.emptyList());
            }
            runIfNotNull(onDone);
        });
    }

    /**
     * Tải chi tiết một item theo ID và set vào {@link ClientModel}.
     *
     * @param itemId ID item cần xem
     * @param onResult callback nhận {@link AuctionItemDTO} ({@code null} nếu không tìm thấy hoặc lỗi)
     */
    public static void loadDetail(int itemId, Consumer<AuctionItemDTO> onResult) {
        if (!AuctionClient.getInstance().isConnected()) {
            LOGGER.warning("loadDetail: client not connected");
            acceptIfNotNull(onResult, null);
            return;
        }

        ServerRequest<Map<String, Integer>> request = ServerRequest
                .<Map<String, Integer>>builder(ActionType.ITEM_GET_DETAIL)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(Map.of("itemId", itemId))
                .build();

        AuctionClient.getInstance().sendRequest(request, response -> {
            if (response.isSuccess()) {
                AuctionItemDTO item = response.getData(AuctionItemDTO.class);
                ClientModel.getInstance().setCurrentAuctionItem(item);
                LOGGER.fine(() -> "Loaded detail for item " + itemId);
                acceptIfNotNull(onResult, item);
            } else {
                LOGGER.warning(() -> "ITEM_GET_DETAIL failed for " + itemId + ": " + response.getMessage());
                ClientModel.getInstance().setCurrentAuctionItem(null);
                acceptIfNotNull(onResult, null);
            }
        });
    }

    /**
     * Tải danh sách comment của một item.
     *
     * @param itemId ID item cần xem comment
     * @param onResult callback nhận danh sách comment
     */
    public static void loadItemComments(int itemId, Consumer<List<CommentDTO>> onResult) {
        ServerRequest<Map<String, Integer>> request = ServerRequest
                .<Map<String, Integer>>builder(ActionType.ITEM_COMMENT)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(Map.of("itemId", itemId))
                .build();
        AuctionClient.getInstance().sendRequest(request, response -> {
            if (response.isSuccess()) {
                List<CommentDTO> comments = GsonUtil.toList(response.getData(), CommentDTO.class);
                acceptIfNotNull(onResult, comments != null ? comments : Collections.emptyList());
            } else {
                LOGGER.warning(() -> "ITEM_COMMENT failed for " + itemId + ": " + response.getMessage());
                acceptIfNotNull(onResult, Collections.emptyList());
            }
        });
    }

    // ========================================
    // REAL-TIME: AUCTION STATUS
    // ========================================

    /**
     * Đăng ký nhận cập nhật trạng thái đấu giá real-time (giá mới).
     * <p>Listener chạy trên FX Thread (do {@link ResponseDispatcher} đảm bảo).
     *
     * @param listener nhận {@link AuctionStatusDTO} khi có bid mới
     * @return wrapper để truyền vào {@link #unsubscribeAuctionStatus} khi cleanup
     */
    public static Consumer<ServerResponse> subscribeAuctionStatus(Consumer<AuctionStatusDTO> listener) {
        if (listener == null) {
            LOGGER.warning("subscribeAuctionStatus: listener null");
            return null;
        }
        Consumer<ServerResponse> wrapper = response -> handleAuctionStatusBroadcast(response, listener);
        ResponseDispatcher.subscribe(EventType.PRICE_UPDATE, wrapper);
        LOGGER.fine("Subscribed PRICE_UPDATE");
        return wrapper;
    }

    /**
     * Hủy đăng ký nhận cập nhật trạng thái đấu giá.
     * Phải truyền đúng wrapper trả về từ {@link #subscribeAuctionStatus}.
     */
    public static void unsubscribeAuctionStatus(Consumer<ServerResponse> wrapper) {
        if (wrapper == null) return;
        ResponseDispatcher.unsubscribe(EventType.PRICE_UPDATE, wrapper);
        LOGGER.fine("Unsubscribed PRICE_UPDATE");
    }

    /**
     * Parse {@link AuctionStatusDTO}, cập nhật ClientModel rồi gọi listener.
     */
    private static void handleAuctionStatusBroadcast(ServerResponse response,
                                                     Consumer<AuctionStatusDTO> listener) {
        PriceUpdateBroadcastResponse update = response.getData(PriceUpdateBroadcastResponse.class);
        if (update == null) {
            LOGGER.fine("PRICE_UPDATE: status null, skip");
            return;
        }
        ClientModel.getInstance().updateItemCurrentPrice(update.getItemId(), update.getNewPrice());
        AuctionStatusDTO status = new AuctionStatusDTO(
            update.getItemId(),
            update.getNewPrice(),
            update.getNewEndTime() != null ? update.getNewEndTime() : java.time.Instant.now(),
            java.util.Collections.emptyList()
        );
        listener.accept(status);
    }

    // ========================================
    // REAL-TIME: AUCTION ENDED
    // ========================================

    /**
     * Đăng ký nhận sự kiện phiên đấu giá kết thúc.
     *
     * @param listener nhận {@link AuctionEndedBroadcastResponse} khi server push AUCTION_ENDED
     * @return wrapper để unsubscribe
     */
    public static Consumer<ServerResponse> subscribeAuctionEnded(Consumer<AuctionEndedBroadcastResponse> listener) {
        if (listener == null) {
            LOGGER.warning("subscribeAuctionEnded: listener null");
            return null;
        }
        Consumer<ServerResponse> wrapper = response -> {
            AuctionEndedBroadcastResponse result = GsonUtil.toObject(response.getData(), AuctionEndedBroadcastResponse.class);
            if (result == null) {
                LOGGER.fine("AUCTION_ENDED: result null, skip");
                return;
            }
            listener.accept(result);
        };
        ResponseDispatcher.subscribe(EventType.AUCTION_ENDED, wrapper);
        LOGGER.fine("Subscribed AUCTION_ENDED");
        return wrapper;
    }

    /**
     * Hủy đăng ký sự kiện kết thúc phiên.
     */
    public static void unsubscribeAuctionEnded(Consumer<ServerResponse> wrapper) {
        if (wrapper == null) return;
        ResponseDispatcher.unsubscribe(EventType.AUCTION_ENDED, wrapper);
        LOGGER.fine("Unsubscribed AUCTION_ENDED");
    }

    // ========================================
    // PRIVATE HELPERS
    // ========================================

    private static void runIfNotNull(Runnable r) {
        if (r != null) r.run();
    }

    private static <T> void acceptIfNotNull(Consumer<T> c, T value) {
        if (c != null) c.accept(value);
    }
}