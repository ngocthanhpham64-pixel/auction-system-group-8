package vn.edu.vnu.uet.group8.server.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.enums.TransactionStatus;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;

/**
 * DAO xử lý toàn bộ thao tác DB liên quan đến BidTransaction.
 *
 * <p>Method {@link #executeBid} là trung tâm — gộp toàn bộ
 * các thao tác cần atomic vào một SQL transaction duy nhất.
 * Connection được mở và đóng tại đây, không leak ra ngoài.
 */
public class BidTransactionDAO {

  private static final Logger logger =
      LoggerFactory.getLogger(BidTransactionDAO.class);

  private final TransactionDAO transactionDAO = new TransactionDAO();

  // ── Record chứa thông tin leader hiện tại ──────────────
  public record LeaderInfo(int bidderId, BigDecimal bidAmount) {}

  // ════════════════════════════════════════════════════════
  // ATOMIC BID EXECUTION
  // Đây là method quan trọng nhất trong toàn bộ hệ thống.
  // Mọi thao tác DB của một lần đặt giá đều nằm ở đây.
  // ════════════════════════════════════════════════════════

  public record BidAction(int bidderId, BigDecimal amount) {}

  /**
   * Thực thi toàn bộ thao tác DB của một cuộc giao tranh đấu giá.
   *
   * @return {@link BidExecutionResult} chứa thông tin sau khi thành công
   */
  public BidExecutionResult executeFightBatch(
      int sessionId,
      BigDecimal newPrice,
      int newLeaderId,
      List<BidAction> bidsToInsert,
      boolean leaderChanged,
      Integer oldLeaderId,
      BigDecimal oldLeaderMax,
      BigDecimal newLeaderMax,
      int bidsToAdd)
      throws SQLException {

    Connection conn = DatabaseConnection.getInstance().getConnection();
    conn.setAutoCommit(false);

    try {
      // -- Bước 1: Hoàn tiền bidder cũ (Nếu có đổi chủ)
      if (leaderChanged && oldLeaderId != null) {
        refundBidder(conn, oldLeaderId, oldLeaderMax, sessionId);
        logger.debug(
            "Hoàn tiền bidderId={}, amount={}",
            oldLeaderId, oldLeaderMax);
      }

      // -- Bước 2: Trừ tiền bidder mới (Nếu có đổi chủ)
      if (leaderChanged) {
        boolean holdBalance = holdBalance(conn, newLeaderId, newLeaderMax, sessionId);
        if (!holdBalance) {
          throw new InsufficientBalanceException(
              "Số dư thay đổi trong quá trình xử lý. Vui lòng thử lại");
        }
      }

      // -- Bước 3: Lưu bid_transaction
      long lastTransactionId = -1;
      for (BidAction action : bidsToInsert) {
        lastTransactionId = insertBidTransaction(conn, action.bidderId(), sessionId, action.amount());
      }

      // -- Bước 4: Cập nhật giá item
      int affected = updateSessionAfterFight(conn, sessionId, newPrice, newLeaderId, bidsToAdd);
      if (affected == 0) {
        throw new BidOutpricedException(
            "Đã có lỗi xảy ra hoặc phiên đã kết thúc. Vui lòng thử lại");
      }

      // -- Bước 5: Đếm tổng bid trong cùng transaction
      int totalBids = countByItemInTx(conn, sessionId);

      conn.commit();

      logger.info(
          "executeFightBatch thành công: txId={}, sessionId={}, newLeader={}, newPrice={}",
          lastTransactionId, sessionId, newLeaderId, newPrice);

      return new BidExecutionResult(lastTransactionId, totalBids);

    } catch (SQLException e) {
      safeRollback(conn);
      throw e;
    } finally {
      safeResetAndClose(conn);
    }
  }


  // ════════════════════════════════════════════════════════
  // QUERY METHODS — không cần transaction riêng
  // ════════════════════════════════════════════════════════

  /**
   * Tìm người đang dẫn đầu (bid cao nhất) của một item.
   * Dùng trước khi gọi {@link #executeBid} để biết ai cần hoàn tiền.
   */
  public Optional<LeaderInfo> findCurrentLeader(int sessionId)
      throws SQLException {
    String sql = """
          SELECT bidder_id, bid_amount
          FROM bid_transaction
          WHERE session_id = ?
          ORDER BY bid_amount DESC
          LIMIT 1
        """;

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new LeaderInfo(
                  rs.getInt("bidder_id"),
                  rs.getBigDecimal("bid_amount")));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Dùng để hoàn tiền cho bidder khi phiên đấu giá bị HỦY (CANCELLED) bởi Admin.
   */
  public void refundBidderExternal(int bidderId, BigDecimal amount, int sessionId) throws SQLException {
    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      refundBidder(conn, bidderId, amount, sessionId);
    }
  }

  /**
   * Đếm tổng số lần bid của một item.
   */
  public int countByItem(int sessionId) throws SQLException {
    String sql =
        "SELECT COUNT(*) FROM bid_transaction WHERE session_id = ?";

    try (Connection conn = DatabaseConnection.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  /**
   * Lấy lịch sử bid của một item, sắp xếp mới nhất lên đầu.
   */
  public List<BidHistoryEntry> findHistoryByItem(int sessionId)
      throws SQLException {
    String sql = """
          SELECT bt.bid_id, bt.session_id, bt.bidder_id, u.username, bt.bid_amount, bt.status, bt.created_at
          FROM bid_transaction bt
          JOIN users u ON bt.bidder_id = u.user_id
          WHERE session_id = ?
          ORDER BY bt.created_at DESC, bt.bid_id DESC
          LIMIT 50
        """;

    List<BidHistoryEntry> history = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          history.add(new BidHistoryEntry(
              rs.getInt("bid_id"),
              rs.getInt("session_id"),
              rs.getInt("bidder_id"),
              rs.getString("username"),
              rs.getBigDecimal("bid_amount"),
              rs.getString("status"),
              rs.getTimestamp("created_at").toInstant()));
        }
      }
    }
    return history;
  }

  public List<UserBidRecord> findHistoryByUser(int userId) throws SQLException {
    String sql = """
        SELECT
          bt.bid_id, bt.session_id, bt.bid_amount, bt.created_at,
          s.item_id, s.current_price, s.end_time,
          i.title AS item_title
        FROM bid_transaction bt
        JOIN auction_session s ON bt.session_id = s.session_id
        JOIN item i ON s.item_id = i.item_id
        WHERE bt.bidder_id = ?
        ORDER BY bt.created_at DESC, bt.bid_id DESC;
        """;
      
    List<UserBidRecord> history = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            history.add(new UserBidRecord(
              rs.getInt("bid_id"),
              rs.getInt("session_id"),
              rs.getInt("item_id"),
              rs.getString("item_title"),
              rs.getBigDecimal("bid_amount"),
              rs.getBigDecimal("current_price"),
              rs.getTimestamp("created_at").toInstant(),
              rs.getTimestamp("end_time").toInstant()
            ));
          }
          return history;
        }
      }
  }

  // ════════════════════════════════════════════════════════
  // PRIVATE — SQL operations trong transaction
  // Tất cả đều nhận Connection từ bên ngoài -- dùng chung transaction
  // ════════════════════════════════════════════════════════

  /**
   * Cập nhật phiên đấu giá sau cuộc chiến.
   *
   * @return số row affected -- 0 nếu phiên đã bị xóa hoặc đóng
   */
  public int updateSessionAfterFight(Connection conn, int sessionId, BigDecimal newPrice, int highestBidderId, int bidsToAdd)
      throws SQLException {
    String sql =
        "UPDATE auction_session"
            + " SET current_price = ?,"
            + "     highest_bidder_id = ?,"
            + "     bid_count = bid_count + ?"
            + " WHERE session_id = ?"
            + "   AND status = 'ACTIVE'"
            + "   AND is_deleted = false";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setBigDecimal(1, newPrice);
      ps.setInt(2, highestBidderId);
      ps.setInt(3, bidsToAdd);
      ps.setInt(4, sessionId);
      return ps.executeUpdate();
    }
  }

  /**
   * Hoàn trả tiền ký quỹ cho người đặt giá cũ khi bị vượt mặt.
   * * <p>Nghiệp vụ này thực hiện:
   * <ol>
   * <li>Cộng lại tiền vào balance và trừ đi ở frozen_balance.</li>
   * <li>Ghi nhận một bản ghi BID_REFUND vào sổ cái giao dịch.</li>
   * </ol>
   *
   * @param conn Kết nối DB đang dùng chung cho transaction.
   * @param bidderId ID của người cần hoàn tiền.
   * @param amount Số tiền đã ký quỹ trước đó.
   * @param sessionId ID của phiên đấu giá liên quan.
   * @throws SQLException Nếu có lỗi truy vấn hoặc vi phạm ràng buộc dữ liệu.
   */
  public void refundBidder(Connection conn, int bidderId, BigDecimal amount, int sessionId)
      throws SQLException {
    String sql = 
        "UPDATE users "
            + "SET balance = balance + ?, "
            + "    frozen_balance = frozen_balance - ? "
            + "WHERE user_id = ? "
            + "  AND frozen_balance >= ? "
            + "  AND is_deleted = false";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setBigDecimal(1, amount);
      ps.setBigDecimal(2, amount);
      ps.setInt(3, bidderId);
      ps.setBigDecimal(4, amount);

      int affectedRows = ps.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Không thể hoàn tiền: Số dư đóng băng không đủ hoặc tài khoản đã bị xóa.");
      }

      // Tạo ID giao dịch duy nhất cho nghiệp vụ hoàn tiền
      String transactionId = UUID.randomUUID().toString();

      // Ghi biên lai vào sổ cái thông qua TransactionDAO
      transactionDAO.insertTransaction(
          conn,
          transactionId,
          bidderId,
          amount,
          TransactionType.BID_REFUND,
          TransactionStatus.SUCCESS,
          "Hoàn trả tiền ký quỹ do bị vượt giá tại phiên #" + sessionId,
          sessionId);

      logger.debug("Đã hoàn tiền thành công cho bidderId={}, amount={}, sessionId={}", 
          bidderId, amount, sessionId);
    }
  }

  /**
   * Thực hiện ký quỹ (hold) số dư của người dùng khi đặt giá.
   * <p>Nghiệp vụ này thực hiện:
   * <ol>
   *   <li>Trừ tiền từ balance và cộng vào frozen_balance.</li>
   *   <li>Ghi nhận một bản ghi BID_HOLD vào sổ cái giao dịch.</li>
   * </ol>
   * @param conn
   * @param bidderId
   * @param amount
   * @param sessionId
   * @return
   * @throws SQLException
   */
  public boolean holdBalance(Connection conn, int bidderId, BigDecimal amount, int sessionId)
      throws SQLException {
    String sql = """
        UPDATE users
        SET balance = balance - ?,
            frozen_balance = frozen_balance + ?
        WHERE user_id = ?
          AND balance >= ?
          AND is_deleted = false
        """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setBigDecimal(1, amount);
      ps.setBigDecimal(2, amount);
      ps.setInt(3, bidderId);
      ps.setBigDecimal(4, amount);
      
      if (ps.executeUpdate() > 0) {
        // TẠO MÃ GIAO DỊCH
        String uuid = UUID.randomUUID().toString();
        
        transactionDAO.insertTransaction(
            conn, uuid, bidderId, amount, 
            TransactionType.BID_HOLD, TransactionStatus.SUCCESS, 
            "Ký quỹ đặt giá", sessionId
        );
        return true;
      }
      return false;
    }
  }

  public long insertBidTransaction(
      Connection conn, int bidderId, int sessionId, BigDecimal bidAmount, String status)
      throws SQLException {
    String sql = """
        INSERT INTO bid_transaction (session_id, bidder_id, bid_amount, status)
        VALUES (?, ?, ?, ?)
        """;

    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, sessionId);
      ps.setInt(2, bidderId);
      ps.setBigDecimal(3, bidAmount);
      ps.setString(4, status);

      int affectedRows = ps.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Không thể lưu lịch sử đặt giá, không có dòng nào được thêm.");
      }

      try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        } else {
          throw new SQLException("Không thể lấy ID của giao dịch đặt giá vừa lưu.");
        }
      }
    }
  }

  // Overload cho tương thích cũ nếu cần (nhưng ta sẽ dùng bản có status cho code mới)
  public long insertBidTransaction(
      Connection conn, int bidderId, int sessionId, BigDecimal bidAmount)
      throws SQLException {
      return insertBidTransaction(conn, bidderId, sessionId, bidAmount, "LEADER");
  }

  /** Đếm bid trong cùng transaction -- tránh đọc stale data. */
  public int countByItemInTx(Connection conn, int sessionId) throws SQLException {
    String sql = "SELECT COUNT(*) FROM bid_transaction WHERE session_id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  // ════════════════════════════════════════════════════════
  // PRIVATE — Connection lifecycle helpers
  // ════════════════════════════════════════════════════════

  private void safeRollback(Connection conn) {
    try {
      conn.rollback();
      logger.warn("Rollback bid transaction thành công");
    } catch (SQLException e) {
      logger.error("Rollback thất bại: {}", e.getMessage());
    }
  }

  private void safeResetAndClose(Connection conn) {
    try {
      // Reset trạng thái trước khi trả về pool
      conn.setAutoCommit(true);
    } catch (SQLException e) {
      logger.error("Không reset được autoCommit: {}", e.getMessage());
    }
    try {
      conn.close();
    } catch (SQLException e) {
      logger.error("Không đóng được connection: {}", e.getMessage());
    }
  }

  // ════════════════════════════════════════════════════════
  // NESTED TYPES — Result và Exception
  // ════════════════════════════════════════════════════════

  /** Kết quả trả về sau khi executeBid thành công. */
  public record BidExecutionResult(long transactionId, int totalBids) {}

  /** Lịch sử một lần bid. */
  public record BidHistoryEntry(
      int bidId,
      int sessionId,
      int bidderId,
      String bidderUsername,
      BigDecimal bidAmount,
      String status,
      Instant bidTime) {}

  /** Lịch sử đấu giá của 1 người cụ thể */
  public record UserBidRecord(
    int bidId,
    int sessionId,
    int itemId,
    String itemTitle,
    BigDecimal bidAmount,
    BigDecimal currentPrice,
    Instant bidTime,
    Instant sessionEndTime
  ) {}

  /** Bid bị vượt qua bởi người khác trong cùng thời điểm. */
  public static class BidOutpricedException extends SQLException {
    public BidOutpricedException(String message) {
      super(message);
    }
  }

  /** Balance thay đổi giữa validate và execute. */
  public static class InsufficientBalanceException extends SQLException {
    public InsufficientBalanceException(String message) {
      super(message);
    }
  }
}