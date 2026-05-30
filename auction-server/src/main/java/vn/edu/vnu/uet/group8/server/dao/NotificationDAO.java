package vn.edu.vnu.uet.group8.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.entity.Notification;

public class NotificationDAO {
  
  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  public List<NotificationDTO> findByUserId(int userId) throws SQLException {
    String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
    List<NotificationDTO> list = new ArrayList<>();
    
    try (Connection conn = getConn(); 
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(NotificationDTO.builder()
              .id(rs.getInt("notification_id"))
              .userId(rs.getInt("user_id"))
              .title(rs.getString("title"))
              .message(rs.getString("message"))
              .type(rs.getString("type"))
              .isRead(rs.getBoolean("is_read"))
              .createdAt(rs.getTimestamp("created_at").toInstant())
              .build());
        }
      }
    }
    return list;
  }

  public NotificationDTO insert(Notification notification) throws SQLException {
    String sql = "INSERT INTO notifications (user_id, title, message, type, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?)";
    
    try (Connection conn = getConn(); 
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, notification.getUserId());
      ps.setString(2, notification.getTitle());
      ps.setString(3, notification.getMessage());
      ps.setString(4, notification.getType().name());
      ps.setBoolean(5, notification.isRead());
      ps.setTimestamp(6, java.sql.Timestamp.from(notification.getCreatedAt()));
      
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          return NotificationDTO.builder()
              .id(keys.getInt(1))
              .userId(notification.getUserId())
              .title(notification.getTitle())
              .message(notification.getMessage())
              .type(notification.getType().name())
              .isRead(notification.isRead())
              .createdAt(notification.getCreatedAt())
              .build();
        }
      }
    }
    throw new SQLException("Không thể lấy ID của thông báo mới");
  }

  public void markAsRead(int notificationId, int userId) throws SQLException {
    String sql = "UPDATE notifications SET is_read = true WHERE notification_id = ? AND user_id = ?";
    
    try (Connection conn = getConn(); 
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, notificationId);
      ps.setInt(2, userId);
      ps.executeUpdate();
    }
  }

  public void delete(int notificationId, int userId) throws SQLException {
    String sql = "DELETE FROM notifications WHERE notification_id = ? AND user_id = ?";
    
    try (Connection conn = getConn(); 
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, notificationId);
      ps.setInt(2, userId);
      ps.executeUpdate();
    }
  }
}
