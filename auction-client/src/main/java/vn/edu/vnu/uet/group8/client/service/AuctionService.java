package vn.edu.vnu.uet.group8.client.service;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.networking.ResponseDispatcher;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.AuctionStatusDTO;
import vn.edu.vnu.uet.group8.common.dto.GetAuctionsRequest;
import vn.edu.vnu.uet.group8.common.dto.ServerRequest;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

import java.util.List;
import java.util.function.Consumer;


/**
 * Service quản lý danh sách và trạng thái phiên đấu giá.
 */
public final class AuctionService {
    /**
     * eventType server push khi có bid mới hoặc trạng thái đấu giá thay đổi.
     */
    public static final String EVENT_AUCTION_STATUS = "auction_status";
    private AuctionService(){}
    /**
     * Tải toàn bộ item đang đấu giá từ server và lưu vào ClientModel.
     * @param filter bộ lọc (category keyword ...); null để lấy tất cả
     * @param onDone gọi khi hoàn thành( thành công lẫn thất bại); null để bỏ qua
     */
    public static void loadAll(GetAuctionsRequest filter, Runnable onDone){
        if(!AuctionClient.getInstance().isConnected()){
            if(onDone != null ) onDone.run();
            return;
        }
        ServerRequest<GetAuctionsRequest> request = ServerRequest
                .<GetAuctionsRequest> builder(ActionType.ITEM_GET_ALL)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(filter)
                .build();
        AuctionClient.getInstance().sendRequest(request,response -> {
            if(response.isSuccess()){
                //Parse List<Item> bằng response data bằng GsonUtil
                String json = GsonUtil.GSON.toJson(response.getData());
                List<Item> items = GsonUtil.toList(response.getData(), Item.class);
                ClientModel.getInstance().setAuctionItems(items);
            }
            if(onDone != null) onDone.run();
        });
    }
    /**
     * Tải chi tiết một item theo ID và set vào ClientModel
     * @param itemId ID item cần xem
     * @param onResult callback nhận Item(null nếu không tìm thấy hoặc lỗi)
     */
    public static void loadDetail(int itemId, Consumer<Item> onResult){
        if(!AuctionClient.getInstance().isConnected()){
            if(onResult != null) onResult.accept(null);
            return;
        }
        ServerRequest<Integer> request = ServerRequest
                .<Integer> builder(ActionType.ITEM_GET_DETAIL)
                .userId(SessionManager.getUserId())
                .token(SessionManager.getAuthToken())
                .payload(itemId)
                .build();
        AuctionClient.getInstance().sendRequest(request,response -> {
            if(response.isSuccess()){
                // Sử dụng getData(Class) của ServerResponse thay vì parse thủ công
                Item item = response.getData(Item.class);
                ClientModel.getInstance().setCurrentAuctionItem(item);
                if(onResult != null) onResult.accept(item);
            }else{
                ClientModel.getInstance().setCurrentAuctionItem(null);
                if(onResult != null) onResult.accept(null);
            }
        });
    }
    /**
     * Đăng ký nhận cập nhật trạng thái đấu giá real time
     * Trả về wrapper để controller dùng cho unsubscribe
     * @param listener nhận AuctionStatusDTO(chạy trên FX Thread)
     * @return wrapper Consumer<ServerResponse> dùng để unsubscribe
     */
    public static Consumer<ServerResponse> subscribeAuctionStatus (Consumer<AuctionStatusDTO> listener){
        Consumer<ServerResponse> wrapper =
                response -> handleAuctionStatusBroadCast(response,listener);
        ResponseDispatcher.subscribe(EVENT_AUCTION_STATUS,wrapper);
        return wrapper;
    }
    /**
     * Hủy đăng kí nhận cập nhật trạng thái.
     * Phải truyền đúng wrapper được trả về từ subscribeAuctionStatus.
     */
    public static void unsubscribeAuctionStatus(Consumer<ServerResponse> wrapper){
        ResponseDispatcher.unsubscribe(EVENT_AUCTION_STATUS,wrapper);
    }

    private static void handleAuctionStatusBroadCast(ServerResponse response,Consumer<AuctionStatusDTO> listener) {
        AuctionStatusDTO status = response.getData(AuctionStatusDTO.class);
        //Thêm null check để tránh NPE khi server trả về data không hợp lệ
        if(status == null){
            return;
        }
        //Cập nhật ClientModel(giá real-time)
        ClientModel.getInstance().updateItemCurrentPrice(status.getItemId(), status.getCurrentPrice());
        listener.accept(status);
    }
}
