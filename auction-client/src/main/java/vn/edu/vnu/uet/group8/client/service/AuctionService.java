package vn.edu.vnu.uet.group8.client.service;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.networking.ResponseDispatcher;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.AuctionEndedDTO;
import vn.edu.vnu.uet.group8.common.dto.AuctionStatusDTO;
import vn.edu.vnu.uet.group8.common.dto.GetAuctionsRequest;
import vn.edu.vnu.uet.group8.common.dto.ServerRequest;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.enums.EventType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

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
            LOGGER.warning("loadAll: client not connected");
            ClientModel.getInstance().setAuctionItems(Collections.emptyList());
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
                List<Item> items = GsonUtil.toList(response.getData(), Item.class);
                if (items == null) items = Collections.emptyList();
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
     * @param onResult callback nhận {@link Item} ({@code null} nếu không tìm thấy hoặc lỗi)
     */
    public static void loadDetail(int itemId, Consumer<Item> onResult) {
        if (!AuctionClient.getInstance().isConnected()) {
            LOGGER.warning("loadDetail: client not connected");
            acceptIfNotNull(onResult, null);
            return;
        }

        ServerRequest<Integer> request = ServerRequest
                .<Integer>builder(ActionType.ITEM_GET_DETAIL)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(itemId)
                .build();

        AuctionClient.getInstance().sendRequest(request, response -> {
            if (response.isSuccess()) {
                Item item = response.getData(Item.class);
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
        AuctionStatusDTO status = response.getData(AuctionStatusDTO.class);
        if (status == null) {
            LOGGER.fine("PRICE_UPDATE: status null, skip");
            return;
        }
        ClientModel.getInstance().updateItemCurrentPrice(status.getItemId(), status.getCurrentPrice());
        listener.accept(status);
    }

    // ========================================
    // REAL-TIME: AUCTION ENDED
    // ========================================

    /**
     * Đăng ký nhận sự kiện phiên đấu giá kết thúc.
     *
     * @param listener nhận {@link AuctionEndedDTO} khi server push AUCTION_ENDED
     * @return wrapper để unsubscribe
     */
    public static Consumer<ServerResponse> subscribeAuctionEnded(Consumer<AuctionEndedDTO> listener) {
        if (listener == null) {
            LOGGER.warning("subscribeAuctionEnded: listener null");
            return null;
        }
        Consumer<ServerResponse> wrapper = response -> {
            AuctionEndedDTO result = GsonUtil.toObject(response.getData(), AuctionEndedDTO.class);
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