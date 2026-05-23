package vn.edu.vnu.uet.group8.server.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.network.RequestParser;
import vn.edu.vnu.uet.group8.server.service.user.NotificationService;

public class NotificationController {
  private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  public ServerResponse handleGetAll(String requestId, int userId) {
    try {
      List<NotificationDTO> notifs = notificationService.getUserNotifications(userId);
      return ServerResponse.reply("NOTIF_GET_ALL", requestId)
          .success(true)
          .data(notifs)
          .build();
    } catch (Exception e) {
      log.error("Lỗi lấy thông báo cho userId={}", userId, e);
      return ServerResponse.replyError("NOTIF_GET_ALL", requestId, "Lỗi: " + e.getMessage());
    }
  }

  public ServerResponse handleMarkRead(JsonObject request, String requestId, int userId) {
    try {
      JsonObject payload = request.has("payload") ? request.getAsJsonObject("payload") : request;
      int notifId = RequestParser.requireInt(payload, "notificationId");
      notificationService.markAsRead(notifId, userId);
      return ServerResponse.reply("NOTIF_MARK_READ", requestId)
          .success(true).message("Đã đánh dấu đã đọc").build();
    } catch (Exception e) {
      return ServerResponse.replyError("NOTIF_MARK_READ", requestId, "Lỗi: " + e.getMessage());
    }
  }

  public ServerResponse handleDelete(JsonObject request, String requestId, int userId) {
    try {
      JsonObject payload = request.has("payload") ? request.getAsJsonObject("payload") : request;
      int notifId = RequestParser.requireInt(payload, "notificationId");
      
      notificationService.deleteNotification(notifId, userId);
      return ServerResponse.reply("NOTIF_DELETE", requestId)
          .success(true).message("Xóa thông báo thành công").build();
    } catch (Exception e) {
      return ServerResponse.replyError("NOTIF_DELETE", requestId, "Lỗi: " + e.getMessage());
    }
  }
}
