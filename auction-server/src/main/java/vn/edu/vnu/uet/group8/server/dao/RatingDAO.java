package vn.edu.vnu.uet.group8.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RatingDAO {
  
  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  public void insertRating(int raterId, int sellerId, int score, String comment) throws SQLException {
    String sql = "INSERT INTO ratings (rater_id, seller_id, score, comment, created_at) VALUES (?, ?, ?, ?, NOW())";
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, raterId);
      ps.setInt(2, sellerId);
      ps.setInt(3, score);
      ps.setString(4, comment);
      ps.executeUpdate();
    }
  }

  public boolean hasRated(int raterId, int sellerId) throws SQLException {
    String sql = "SELECT 1 FROM ratings WHERE rater_id = ? AND seller_id = ?";
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, raterId);
      ps.setInt(2, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  public boolean hasBoughtFrom(int buyerId, int sellerId) throws SQLException {
    String sql = """
        SELECT 1 FROM auction_session s
        JOIN item i ON s.item_id = i.item_id
        WHERE s.status = 'SOLD' AND s.highest_bidder_id = ? AND i.seller_id = ?
        LIMIT 1
        """;
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, buyerId);
      ps.setInt(2, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }
}
