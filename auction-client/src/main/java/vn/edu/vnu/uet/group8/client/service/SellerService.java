package vn.edu.vnu.uet.group8.client.service;

import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.request.ServerRequest;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SellerService {

    private SellerService() {
    }

    public static void createItem(
            CreateItemRequest request,
            Consumer<Item> onResult,
            Consumer<String> onFailure
    ) {

        Consumer<Item> success =
                onResult != null ? onResult : item -> {};

        Consumer<String> fail =
                onFailure != null ? onFailure : msg -> {};

        if (!AuctionClient.getInstance().isConnected()) {
            fail.accept("Không có kết nối server");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", request.name);
        payload.put("category", request.category);
        payload.put("condition", request.condition);
        payload.put("description", request.description);
        payload.put("specs", request.specs);
        payload.put("imageUrls", request.imageUrls);
        payload.put("startPrice", request.startPrice);
        payload.put("bidStep", request.bidStep);
        payload.put("durationHours", request.durationHours);

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.ITEM_CREATE,
                payload,
                response -> {

                    if (response.isSuccess()) {

                        Item createdItem =
                                GsonUtil.toObject(
                                        response.getData(),
                                        Item.class
                                );

                        success.accept(createdItem);

                    } else {

                        fail.accept(response.getMessage());
                    }
                }
        );
    }

    public static void updateItem(
            int itemId,
            CreateItemRequest request,
            Consumer<Boolean> onResult
    ) {

        Consumer<Boolean> result =
                onResult != null ? onResult : ok -> {};

        if (!AuctionClient.getInstance().isConnected()) {
            result.accept(false);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("itemId", itemId);

        if (request.name != null) {
            payload.put("title", request.name);
        }

        if (request.description != null) {
            payload.put("description", request.description);
        }

        if (request.condition != null) {
            payload.put("condition", request.condition);
        }

        if (request.specs != null) {
            payload.put("specs", request.specs);
        }

        if (request.imageUrls != null) {
            payload.put("imageUrls", request.imageUrls);
        }

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.ITEM_UPDATE,
                payload,
                response -> result.accept(response.isSuccess())
        );
    }

    public static void deleteItem(
            int itemId,
            Consumer<Boolean> onResult
    ) {

        Consumer<Boolean> result =
                onResult != null ? onResult : ok -> {};

        if (!AuctionClient.getInstance().isConnected()) {
            result.accept(false);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("itemId", itemId);

        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.ITEM_DELETE,
                payload,
                response -> result.accept(response.isSuccess())
        );
    }

    public static void getMyListings(
            Consumer<List<AuctionItemDTO>> onResult,
            Consumer<String> onFailure
    ) {

        Consumer<List<AuctionItemDTO>> success =
                onResult != null ? onResult : items -> {};

        Consumer<String> fail =
                onFailure != null ? onFailure : msg -> {};

        if (!AuctionClient.getInstance().isConnected()) {
            fail.accept("Không có kết nối server");
            return;
        }

        ServerRequest<Void> req =
                ServerRequest
                        .<Void>builder(ActionType.ITEM_MY_LISTINGS)
                        .userId(SessionManager.getUserId())
                        .token(SessionManager.getAuthToken())
                        .build();

        AuctionClient.getInstance().sendRequest(req, response -> {

            if (response.isSuccess()) {

                List<AuctionItemDTO> items =
                        GsonUtil.toList(
                                response.getData(),
                                AuctionItemDTO.class
                        );

                success.accept(items != null ? items : List.of());

            } else {

                fail.accept(response.getMessage());
            }
        });
    }

    public static class CreateItemRequest {

        public String name;
        public String category;
        public String condition;
        public String description;
        public BigDecimal startPrice;
        public BigDecimal bidStep;
        public int durationHours;
        public Map<String, String> specs;
        public List<String> imageUrls;
        public boolean hasCert;
        public String certBody;
        public String certId;

        public CreateItemRequest(
                String name,
                String category,
                String condition,
                String description,
                BigDecimal startPrice,
                BigDecimal bidStep,
                int durationHours,
                Map<String, String> specs,
                List<String> imageUrls,
                boolean hasCert,
                String certBody,
                String certId
        ) {
            this.name = name;
            this.category = category;
            this.condition = condition;
            this.description = description;
            this.startPrice = startPrice;
            this.bidStep = bidStep;
            this.durationHours = durationHours;
            this.specs = specs;
            this.imageUrls = imageUrls;
            this.hasCert = hasCert;
            this.certBody = certBody;
            this.certId = certId;
        }
    }
}