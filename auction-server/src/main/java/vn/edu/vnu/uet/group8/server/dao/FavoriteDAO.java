package vn.edu.vnu.uet.group8.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDAO {
  
  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  public void addFavorite(int userId, int itemId) throws SQLException {
    String sql = "INSERT IGNORE INTO favorites (user_id, item_id, created_at) VALUES (?, ?, NOW())";
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      ps.setInt(2, itemId);
      ps.executeUpdate();
    }
  }

  public void removeFavorite(int userId, int itemId) throws SQLException {
    String sql = "DELETE FROM favorites WHERE user_id = ? AND item_id = ?";
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      ps.setInt(2, itemId);
      ps.executeUpdate();
    }
  }

  public List<Integer> getFavoriteItemIds(int userId) throws SQLException {
    String sql = "SELECT item_id FROM favorites WHERE user_id = ? ORDER BY created_at DESC";
    List<Integer> list = new ArrayList<>();
    
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(rs.getInt("item_id"));
        }
      }
    }
    return list;
  }
}
