package vn.edu.vnu.uet.group8.client.service;

import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.common.dto.ServerRequest;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.entity.Item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * SellerService — quản lý sản phẩm của seller.
 *
 * ⚠️ PHỤ THUỘC BE BỔ SUNG ActionType:
 *   - ITEM_CREATE
 *   - ITEM_UPDATE
 *   - ITEM_DELETE
 *   - ITEM_MY_LISTINGS
 *
 * BE đã có service class tương ứng (ItemWriteService, ItemQueryService.getMyItems),
 * chỉ cần expose qua ActionType + ClientHandler dispatch.
 *
 * Khi BE bổ sung → uncomment các method gọi sendRequest.
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

        // TODO: BE bổ sung ActionType.ITEM_CREATE
        // ServerRequest<CreateItemRequest> req = ServerRequest
        //         .<CreateItemRequest>builder(ActionType.ITEM_CREATE)
        //         .payload(request)
        //         .build();
        //
        // AuctionClient.getInstance().sendAuthenticatedRequest(req, response -> {
        //     if (response.isSuccess()) {
        //         Item item = response.getData(Item.class);
        //         onResult.accept(item);
        //     } else {
        //         onFailure.accept(response.getMessage());
        //     }
        // });

        onFailure.accept("BE chưa expose ActionType.ITEM_CREATE");
    }

    /**
     * Cập nhật sản phẩm.
     */
    public static void updateItem(int itemId, CreateItemRequest request,
                                  Consumer<Boolean> onResult) {
        // TODO: BE bổ sung ActionType.ITEM_UPDATE
        onResult.accept(false);
    }

    /**
     * Xóa sản phẩm (chỉ cho phép khi chưa có bid).
     */
    public static void deleteItem(int itemId,
                                  Consumer<Boolean> onResult) {
        // TODO: BE bổ sung ActionType.ITEM_DELETE
        onResult.accept(false);
    }

    /**
     * Lấy danh sách sản phẩm của user hiện tại.
     */
    public static void getMyListings(Consumer<List<Item>> onResult,
                                     Consumer<String> onFailure) {
        if (!AuctionClient.getInstance().isConnected()) {
            onFailure.accept("Không có kết nối server");
            return;
        }

        // TODO: BE bổ sung ActionType.ITEM_MY_LISTINGS
        // ServerRequest<Void> req = ServerRequest
        //         .<Void>builder(ActionType.ITEM_MY_LISTINGS)
        //         .build();
        //
        // AuctionClient.getInstance().sendAuthenticatedRequest(req, response -> {
        //     if (response.isSuccess()) {
        //         List<Item> items = response.getDataList(Item.class);
        //         onResult.accept(items);
        //     } else {
        //         onFailure.accept(response.getMessage());
        //     }
        // });

        // Tạm thời trả về list rỗng
        onResult.accept(List.of());
    }

    // ===== INNER CLASS — DTO REQUEST =====

    /**
     * DTO request để tạo/sửa item.
     *
     * ⚠️ BE nên tạo class tương đương trong auction-common/dto:
     *   public class CreateItemRequest { String name, category, ...; }
     *
     * Khi BE có → đổi inner class này thành import từ common.
     */
    public static class CreateItemRequest {
        public String name;
        public String category;
        public String condition;
        public String description;
        public BigDecimal startPrice;
        public BigDecimal bidStep;
        public int durationHours;
        public Map<String, String> specs;  // brand, model, year, material, origin
        public boolean hasCert;
        public String certBody;
        public String certId;

        public CreateItemRequest(String name, String category, String condition,
                                 String description, BigDecimal startPrice,
                                 BigDecimal bidStep, int durationHours,
                                 Map<String, String> specs,
                                 boolean hasCert, String certBody, String certId) {
            this.name = name;
            this.category = category;
            this.condition = condition;
            this.description = description;
            this.startPrice = startPrice;
            this.bidStep = bidStep;
            this.durationHours = durationHours;
            this.specs = specs;
            this.hasCert = hasCert;
            this.certBody = certBody;
            this.certId = certId;
        }
    }
}