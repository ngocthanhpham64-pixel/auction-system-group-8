package vn.edu.vnu.uet.group8.client.service;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.networking.ResponseDispatcher;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.NotificationDTO;
import vn.edu.vnu.uet.group8.common.dto.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

import java.util.List;
import java.util.function.Consumer;

public final class NotificationService {
    /** EventType server push - TODO: xác nhận với backend chuỗi chính xác*/
    public static final String EVENT_NOTIFICATION = "notification";
    private NotificationService(){}
    public static void loadAll(Consumer<List<NotificationDTO>>onSuccess, Consumer<String> onFailure){
        if(!SessionManager.isLoggedIn()){
            if(onFailure != null) onFailure.accept("Bạn cần đăng nhập");
            return;
        }
        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.NOTIF_GET_ALL,null,
                response -> {
                    if(response.isSuccess()){
                        List<NotificationDTO> list = GsonUtil.toList(response.getData(),NotificationDTO.class);
                        ClientModel.getInstance().setNotifications(list);
                        if(onSuccess!=null) onSuccess.accept(list);
                    } else{
                        if(onFailure != null) onFailure.accept(response.getMessage());
                    }
                });
    }
    public static void markRead(int notificationId, Runnable onSuccess, Consumer<String> onFailure){
        if(!SessionManager.isLoggedIn()){
            if(onFailure != null) onFailure.accept("Bạn cần đăng nhập");
            return;
        }
        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.NOTIF_MARK_READ, notificationId,
                response -> {
                    if(response.isSuccess()){
                        //Cập nhật ClientModel để bagde tự giảm
                        ClientModel.getInstance().markNotificationRead(notificationId);
                        if(onSuccess != null) onSuccess.run();
                    } else{
                        if(onFailure != null) onFailure.accept(response.getMessage());
                    }
                });
    }
    /**
     * Đăng ký nhận thông báo push real-time
     * Pattern giống (AuctionService.subscribeAuctionStatus)
     * Tự động parse (NotificationDTO), cập nhật (ClientModel) sau đó gọi listener với object đã parse
     * @param onNew listener nhận NotificationDTO (chạy trên UI Thread)
     * @return wrapper dùng để hủy đăng ký
     */
    public static Consumer<ServerResponse> subscribePush(Consumer<NotificationDTO> onNew){
        Consumer<ServerResponse> wrapper = response -> {
            NotificationDTO notif = GsonUtil.toObject(response.getData(), NotificationDTO.class);
            if(notif == null) return;
            // Tự động thêm vào ClientModel -> badge tự động tăng qua binding
            ClientModel.getInstance().addNotification(notif);
            onNew.accept(notif);
        };
        ResponseDispatcher.subscribe(EVENT_NOTIFICATION,wrapper);
        return wrapper;
    }
    public static void unsubscribePush(Consumer<ServerResponse> wrapper){
        ResponseDispatcher.unsubscribe(EVENT_NOTIFICATION, wrapper);
    }
}
