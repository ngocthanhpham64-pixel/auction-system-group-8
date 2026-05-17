package vn.edu.vnu.uet.group8.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;

public class AutoBidDAO {
  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  public void saveConfig(AutoBidConfig config) throws SQLException {
    String deactivateSql = "UPDATE auto_bid_config SET is_active = false WHERE session_id = ? AND user_id = ?";
    String insertSql = """
        INSERT INTO auto_bid_config (session_id, user_id, max_price, is_active, created_at)
        VALUES (?, ?, ?, ?, ?)
        """;

    try (Connection conn = getConn()) {
      conn.setAutoCommit(false);
      try (PreparedStatement psDeactivate = conn.prepareStatement(deactivateSql);
           PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
        psDeactivate.setInt(1, config.getSessionId());
        psDeactivate.setInt(2, config.getUserId());
        psDeactivate.executeUpdate();

        psInsert.setInt(1, config.getSessionId());
        psInsert.setInt(2, config.getUserId());
        psInsert.setBigDecimal(3, config.getMaxPrice());
        psInsert.setBoolean(4, config.isActive());
        psInsert.setTimestamp(5, Timestamp.from(config.getCreatedAt()));
        psInsert.executeUpdate();

        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  public void deactivate(int configId) throws SQLException {
    String sql = "UPDATE auto_bid_config SET is_active = false WHERE config_id = ?";
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, configId);
      ps.executeUpdate();
    }
  }

  public List<AutoBidConfig> findActiveBySession(int sessionId) throws SQLException {
    String sql = "SELECT * FROM auto_bid_config WHERE session_id = ? AND is_active = true ORDER BY max_price DESC, created_at ASC";
    List<AutoBidConfig> configs = new ArrayList<>();
    // ... Parse Result ... (Tôi viết thu gọn để tối ưu)
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          configs.add(AutoBidConfig.reconstructor()
              .id(rs.getInt("config_id")).sessionId(rs.getInt("session_id"))
              .userId(rs.getInt("user_id")).maxPrice(rs.getBigDecimal("max_price"))
              .isActive(rs.getBoolean("is_active")).createdAt(rs.getTimestamp("created_at").toInstant())
              .isDeleted(false).build());
        }
      }
    }
    return configs;
  }
}
