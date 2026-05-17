package vn.edu.vnu.uet.group8.server.service.user;

import java.sql.SQLException;
import java.util.List;

import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.entity.Notification;
import vn.edu.vnu.uet.group8.common.enums.NotificationType;
import vn.edu.vnu.uet.group8.server.dao.NotificationDAO;

public class NotificationService {
  private final NotificationDAO notificationDAO;

  public NotificationService(NotificationDAO notificationDAO) {
    this.notificationDAO = notificationDAO;
  }

  public List<NotificationDTO> getUserNotifications(int userId) throws SQLException {
    return notificationDAO.findByUserId(userId);
  }

  public void markAsRead(int notificationId, int userId) throws SQLException {
    notificationDAO.markAsRead(notificationId, userId);
  }

  public void deleteNotification(int notificationId, int userId) throws SQLException {
    notificationDAO.delete(notificationId, userId);
  }

  public NotificationDTO createNotification(int userId, String title, String message, NotificationType type) throws SQLException {
    Notification notif = Notification.builder()
        .userId(userId)
        .title(title)
        .message(message)
        .type(type)
        .build();
    return notificationDAO.insert(notif);
  }
}
