package vn.edu.vnu.uet.group8.client.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.BidRecord;
import vn.edu.vnu.uet.group8.common.dto.request.AutoBidRequest;
import vn.edu.vnu.uet.group8.common.dto.request.BidRequest;
import vn.edu.vnu.uet.group8.common.dto.request.ServerRequest;
import vn.edu.vnu.uet.group8.common.dto.response.BidResponse;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * Service xử lý đặt giá thủ công, auto-bid và lịch sử đặt giá.
 */
public final class BidService {

    private BidService() {}

    private static <T> void safeAccept(Consumer<T> consumer, T value) {
        if (consumer != null) {
            consumer.accept(value);
        }
    }

    /**
     * Đặt giá thủ công cho một phiên đấu giá.
     * @param itemId ID sản phẩm
     * @param amount số tiền muốn đặt (phải > 0)
     * @param onResult callback nhận BidResponse
     */
    public static void placeBid(
            int itemId,
            BigDecimal amount,
            Consumer<BidResponse> onResult
    ) {

        // Validate sớm
        if (!SessionManager.isLoggedIn()) {
            safeAccept(
                    onResult,
                    new BidResponse(false,
                            "Bạn cần đăng nhập để đặt giá")
            );
            return;
        }

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {

            safeAccept(
                    onResult,
                    new BidResponse(false,
                            "Số tiền không hợp lệ")
            );
            return;
        }

        // Connection guard
        if (!AuctionClient.getInstance().isConnected()) {
            safeAccept(
                    onResult,
                    new BidResponse(false,
                            "Không có kết nối đến server")
            );
            return;
        }

        BidRequest payload = new BidRequest(
                itemId,
                SessionManager.getUserId(),
                amount
        );

        ServerRequest<BidRequest> request =
                ServerRequest.<BidRequest>builder(ActionType.BID_PLACE)
                        .userId(SessionManager.getUserId())
                        .token(SessionManager.getAuthToken())
                        .payload(payload)
                        .build();

        AuctionClient.getInstance().sendRequest(request, response -> {

            if (response.isSuccess()) {

                BidResponse result = GsonUtil.toObject(
                        response.getData(),
                        BidResponse.class
                );

                if (result == null) {
                    result = new BidResponse(
                            true,
                            "Đặt giá thành công"
                    );
                }

                safeAccept(onResult, result);

            } else {

                String msg = response.getMessage() != null
                        ? response.getMessage()
                        : "Đặt giá thất bại";

                safeAccept(
                        onResult,
                        new BidResponse(false, msg)
                );
            }
        });
    }

    /**
     * Bật auto-bid cho một phiên đấu giá.
     *
     * @param itemId ID sản phẩm
     * @param maxAmount giá trần tối đa
     * @param onResult callback nhận true nếu thành công
     */
    public static void setAutoBid(
            int itemId,
            BigDecimal maxAmount,
            Consumer<Boolean> onResult
    ) {

        if (!SessionManager.isLoggedIn()) {
            safeAccept(onResult, false);
            return;
        }

        // Validate
        if (maxAmount == null
                || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {

            safeAccept(onResult, false);
            return;
        }

        if (!AuctionClient.getInstance().isConnected()) {
            safeAccept(onResult, false);
            return;
        }

        AutoBidRequest payload = AutoBidRequest.builder()
                .itemId(itemId)
                .userId(SessionManager.getUserId())
                .maxPrice(maxAmount)
                .build();

        ServerRequest<AutoBidRequest> request =
                ServerRequest.<AutoBidRequest>builder(ActionType.BID_AUTO)
                        .userId(SessionManager.getUserId())
                        .token(SessionManager.getAuthToken())
                        .payload(payload)
                        .build();

        AuctionClient.getInstance().sendRequest(
                request,
                response -> safeAccept(onResult, response.isSuccess())
        );
    }

    /**
     * Tải lịch sử đặt giá của một phiên đấu giá.
     *
     * @param itemId ID sản phẩm
     * @param onResult callback nhận List<BidRecord>
     */
    public static void loadHistory(
            int itemId,
            Consumer<List<BidRecord>> onResult
    ) {

        if (!AuctionClient.getInstance().isConnected()) {
            safeAccept(onResult, List.of());
            return;
        }

        ServerRequest<Map<String, Integer>> request =
                ServerRequest
                        .<Map<String, Integer>>builder(
                                ActionType.BID_HISTORY
                        )
                        .userId(SessionManager.getUserId())
                        .token(SessionManager.getAuthToken())
                        .payload(Map.of("itemId", itemId))
                        .build();

        AuctionClient.getInstance().sendRequest(request, response -> {

            if (response.isSuccess()) {

                List<BidRecord> records = GsonUtil.toList(
                        response.getData(),
                        BidRecord.class
                );

                safeAccept(
                        onResult,
                        records != null ? records : List.of()
                );

            } else {

                safeAccept(onResult, List.of());
            }
        });
    }
}