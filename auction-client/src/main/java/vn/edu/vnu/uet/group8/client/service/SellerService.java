package vn.edu.vnu.uet.group8.client.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.request.CreateItemRequest;
import vn.edu.vnu.uet.group8.common.dto.request.ServerRequest;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * SellerService — quản lý sản phẩm của seller.
 *
 */
public final class SellerService {

    private SellerService() {}

    /**
     * Đăng sản phẩm mới.
     *
     * @param request thông tin sản phẩm cần đăng
     * @param onResult callback nhận Item đã tạo (có id)
     */
    public static void createItem(CreateItemRequest request,
                                  Consumer<Item> onResult,
                                  Consumer<String> onFailure) {
        if (!AuctionClient.getInstance().isConnected()) {
            onFailure.accept("Không có kết nối server");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", request.name);
        payload.put("category", request.category);
        payload.put("condition", request.condition);
        payload.put("description", request.description);
        // payload.put("specs", request.specs);
        payload.put("imageUrls", request.imageUrls);
        // Note: Client truyền lên giá khởi điểm, khoảng thời gian đấu giá.
        payload.put("startPrice", request.startPrice);
        payload.put("bidStep", request.bidStep);
        payload.put("durationHours", request.durationMinutes);
        payload.put("startTime", request.startTime);

        AuctionClient.getInstance().sendAuthenticatedRequest(ActionType.ITEM_CREATE, payload, response -> {
            if (response.isSuccess()) {
                Item createdItem = GsonUtil.toObject(response.getData(), Item.class);
                onResult.accept(createdItem);
            } else {
                onFailure.accept(response.getMessage());
            }
        });
    }

    /**
     * Cập nhật sản phẩm.
     */
    public static void updateItem(int itemId, CreateItemRequest request,
                                  Consumer<Boolean> onResult) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("itemId", itemId);
        if (request.name != null) payload.put("title", request.name);
        if (request.description != null) payload.put("description", request.description);
        if (request.condition != null) payload.put("condition", request.condition);
        // if (request.specs != null) payload.put("specs", request.specs);
        if (request.imageUrls != null) payload.put("imageUrls", request.imageUrls);

        AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.ITEM_UPDATE, payload, response -> {
                onResult.accept(response.isSuccess());
            }
        );
    }

    /**
     * Xóa sản phẩm (chỉ cho phép khi chưa có bid).
     */
    public static void deleteItem(int itemId,
                                  Consumer<Boolean> onResult) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("itemId", itemId);

        AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.ITEM_DELETE, payload, response -> {
                onResult.accept(response.isSuccess());
            }
        );
    }

    /**
     * Lấy danh sách sản phẩm của user hiện tại.
     */
    public static void getMyListings(Consumer<List<AuctionItemDTO>> onResult,
                                     Consumer<String> onFailure) {
        if (!AuctionClient.getInstance().isConnected()) {
            onFailure.accept("Không có kết nối server");
            return;
        }

        ServerRequest<Void> req = ServerRequest
                .<Void>builder(ActionType.ITEM_MY_LISTINGS)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .build();
        
        AuctionClient.getInstance().sendRequest(req, response -> {
            if (response.isSuccess()) {
                List<AuctionItemDTO> items = GsonUtil.toList(response.getData(), AuctionItemDTO.class);
                onResult.accept(items != null ? items : List.of());
            } else {
                onFailure.accept(response.getMessage());
            }
        });
    }
}