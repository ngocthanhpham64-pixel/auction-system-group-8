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
    /**
     * Đặt giá thủ công cho một phiên đấu giá.
     * @param itemId ID sản phẩm
     * @param amount số tiền muốn đặt(phải>0)
     * @param onResult callback nhận (BidResponse) chạy trên FX Thread
     */
    public static void placeBid(int itemId, BigDecimal amount,Consumer<BidResponse> onResult ){
        //Validate sớm
        if(!SessionManager.isLoggedIn()){
            onResult.accept(new BidResponse(false, "Bạn cần đăng nhập để đặt giá"));
            return;
        }
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            onResult.accept(new BidResponse(false,"Số tiền không hợp lệ"));
            return;
        }
        //Connection guard
        if (!AuctionClient.getInstance().isConnected()){
            onResult.accept(new BidResponse(false,"Không có kết nối đến server "));
            return;
        }
        BidRequest payload = new BidRequest(itemId,SessionManager.getUserId(),amount);
        ServerRequest<BidRequest> request = ServerRequest
                .<BidRequest> builder(ActionType.BID_PLACE)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(payload)
                .build();
        AuctionClient.getInstance().sendRequest(request,response -> {
            if(response.isSuccess()){
                BidResponse result = GsonUtil.toObject(response.getData(),BidResponse.class);
                if(result == null){
                    result = new BidResponse(true,"Đặt giá thành công");
                }
                onResult.accept(result);
            } else{
                String msg = response.getMessage() != null ? response.getMessage() : "Đặt giá thất bại";
                onResult.accept(new BidResponse(false,msg));
            }
        });
    }
    /**
     * Bật auto-bid cho một phiên đấu giá.
     * -Lưu ý: maxAmount phải > 0. Để tắt auto-bid cần một action riêng
     * (hiện tại chưa triển khai). Không dùng null để tắt.
     * @param itemId ID sản phẩm
     * @param maxAmount giá trần tối đa user chịu trả(phải>0)
     * @param onResult callback nhận true nếu thành công
     */
    public static void setAutoBid(int itemId,BigDecimal maxAmount,Consumer<Boolean> onResult){
        if(!SessionManager.isLoggedIn()){
            onResult.accept(false);
            return;
        }
        //Validate: maxAmount bắt buộc > 0, không cho null
        if(maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO)<=0){
            onResult.accept(false);
            return;
        }
        if(!AuctionClient.getInstance().isConnected()){
            onResult.accept(false);
            return;
        }
        AutoBidRequest payload = AutoBidRequest.builder()
                .itemId(itemId)
                .userId(SessionManager.getUserId())
                .maxPrice(maxAmount)
                .build();
        ServerRequest<AutoBidRequest> request = ServerRequest.<AutoBidRequest> builder(ActionType.BID_AUTO)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(payload)
                .build();
        AuctionClient.getInstance().sendRequest(request,response -> {
            onResult.accept(response.isSuccess());
        });
    }
    /**
     * Tải lịch sử đặt giá của một phiên đấu giá
     * @param itemId ID sản phẩm
     * @param onResult callback nhận List<BidRecord>(rỗng nếu không có hoặc lỗi)
     */
    public static void loadHistory(int itemId,Consumer<List<BidRecord>> onResult){
        if(!AuctionClient.getInstance().isConnected()){
            onResult.accept(List.of());
            return;
        }
        ServerRequest<Map<String, Integer>> request = ServerRequest
                .<Map<String, Integer>> builder(ActionType.BID_HISTORY)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(Map.of("itemId", itemId))
                .build();
        AuctionClient.getInstance().sendRequest(request,response -> {
            if(response.isSuccess()){
                List<BidRecord> records = GsonUtil.toList(response.getData(),BidRecord.class);
                onResult.accept(records != null ? records : List.of());
            } else{
                onResult.accept(List.of());
            }
        });
    }
}
