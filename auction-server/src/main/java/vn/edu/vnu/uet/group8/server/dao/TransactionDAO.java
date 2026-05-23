package vn.edu.vnu.uet.group8.server.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.model.TransactionHistoryEntry;
import vn.edu.vnu.uet.group8.common.dto.model.TransactionRecord;
import vn.edu.vnu.uet.group8.common.enums.TransactionStatus;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;


/**
 * DAO xử lý truy vấn lịch sử giao dịch (wallet_transaction).
 */
public class TransactionDAO {
  private static final Logger log = LoggerFactory.getLogger(TransactionDAO.class);

  // ═══════════════════════════════════════════════════
  // Get connection
  //═══════════════════════════════════════════════════
  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  // ═══════════════════════════════════════════════════
  // Nghiệp vụ chính 
  // ═══════════════════════════════════════════════════
  /**
   * Lưu giao dịch vào DB.
   *
   * @param conn
   * @param transactionId
   * @param userId
   * @param amount
   * @param transactionType
   * @param status
   * @param description
   * @param sessionId
   * @throws SQLException
   */
  public void insertTransaction(
    Connection conn,
    String transactionId,
    int userId,
    BigDecimal amount,
    TransactionType transactionType,
    TransactionStatus status,
    String description,
    Integer sessionId
  ) throws SQLException {
    String sql = """
        INSERT INTO wallet_transaction
          (transaction_id, user_id, amount, transaction_type,
          status, description, session_id, created_at)
        VALUES (?,?,?,?,?,?,?,NOW())
        """;
    
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, transactionId);
      ps.setInt(2, userId);
      ps.setBigDecimal(3, amount);
      ps.setString(4, transactionType.name());
      ps.setString(5, status != null ? status.name() : TransactionStatus.SUCCESS.name());
      ps.setString(6, description == null ? "" : description);
      ps.setInt(7, sessionId == null ? 0 : sessionId);
      ps.executeUpdate();
    }
  }

  // ═══════════════════════════════════════════════════
  // Tìm kiếm
  // ═══════════════════════════════════════════════════
  /**
   * Lấy lịch sử giao dịch của một người dùng, sắp xếp mới nhất lên đầu.
   */
  public List<TransactionHistoryEntry> getTransactionsByUserId(int userId) throws SQLException {
    String sql = """
        SELECT transaction_id, amount, transaction_type, description, created_at
        FROM wallet_transaction
        WHERE user_id = ?
        ORDER BY created_at DESC
        """;

    List<TransactionHistoryEntry> history = new ArrayList<>();
    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          history.add(new TransactionHistoryEntry(
              rs.getString("transaction_id"),
              rs.getBigDecimal("amount"),
              parseToTransactionType(rs.getString("transaction_type")),
              rs.getString("description"),
              rs.getTimestamp("created_at").toInstant()));
        }
      }
    }
    return history;
  }

  public List<TransactionRecord> getTransactionsBySessionId(int sessionId) {
    String sql = """
        SELECT transaction_id, user_id, amount, transaction_type,
          status, description, session_id, created_at
        FROM wallet_transaction
        WHERE session_id = ?
        ORDER BY created_at ASC
        """;
    
    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        List<TransactionRecord> records = new ArrayList<>();
        while (rs.next()) {
          records.add(new TransactionRecord(
            rs.getString("transaction_id"),
            rs.getInt("user_id"),
            rs.getBigDecimal("amount"),
            parseToTransactionType(rs.getString("transaction_type")),
            rs.getTimestamp("created_at").toInstant(),
            TransactionStatus.valueOf(rs.getString("status")),
            rs.getString("description"),
            rs.getInt("session_id")
          ));
        }
        return records;
      }
    } catch (SQLException e) {
      log.error("Lỗi khi lấy giao dịch theo sessionId: {}", sessionId, e);
      return new ArrayList<>();
    
      }
    }

  // ═══════════════════════════════════════════════════
  // HELPER
  // ═══════════════════════════════════════════════════
  private TransactionType parseToTransactionType(String type) {
    if (type == null || type.isBlank()) {
      return null;
    }
    try {
      return TransactionType.valueOf(type);
    } catch (IllegalArgumentException e) {
      log.warn("Unknown transaction type: {}", type);
      return null;
    }
  }

  public boolean existsTransaction(String transactionId) throws SQLException {
    String sql = """
        SELECT EXISTS(
            SELECT 1 FROM wallet_transaction
            WHERE transaction_id = ?
        )
        """;

    try (Connection conn = getConn(); 
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, transactionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getBoolean(1);
      }
    }
  }

  public boolean existsTransaction(Connection conn, String transactionId) throws SQLException {
    String sql = """
        SELECT EXISTS(
            SELECT 1 FROM wallet_transaction
            WHERE transaction_id = ?
        )
        """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, transactionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getBoolean(1);
      }
    }
  }
}
