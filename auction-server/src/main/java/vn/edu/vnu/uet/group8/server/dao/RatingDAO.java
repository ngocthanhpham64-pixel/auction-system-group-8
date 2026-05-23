package vn.edu.vnu.uet.group8.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import vn.edu.vnu.uet.group8.common.dto.model.ReviewDTO;

public class RatingDAO {
  
  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  public void insertRating(int raterId, String raterUsername, int sellerId, int score, String comment) throws SQLException {
    String sql = """
        INSERT INTO ratings (buyer_id, buyer_username, seller_id, score, comment, created_at) 
        VALUES (?, ?, ?, ?, ?, NOW())
        """;
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, raterId);
      ps.setString(2, raterUsername);
      ps.setInt(3, sellerId);
      ps.setInt(4, score);
      ps.setString(5, comment);
      ps.executeUpdate();
    }
  }

  public boolean hasRated(int raterId, int sellerId) throws SQLException {
    String sql = "SELECT 1 FROM ratings WHERE buyer_id = ? AND seller_id = ?";
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, raterId);
      ps.setInt(2, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  public List<ReviewDTO> getSellerReviews(int sellerId) throws SQLException {
    String sql = """
        SELECT u.username, r.score, r.comment, r.created_at
        FROM ratings r
        JOIN users u ON r.buyer_id = u.user_id
        WHERE r.seller_id = ?
        ORDER BY r.created_at DESC
        """;

    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      List<ReviewDTO> list = new ArrayList<>();
      ps.setInt(1, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Instant createdAt = rs.getTimestamp("created_at") != null
              ? rs.getTimestamp("created_at").toInstant() : null;
          list.add(new ReviewDTO(
              rs.getString("username"),
              rs.getInt("score"),
              rs.getString("comment"),
              createdAt));
        }
        return list;
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
