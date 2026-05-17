package vn.edu.vnu.uet.group8.server.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

  /**
   * Thực thi toàn bộ thao tác DB của một lần đặt giá.
   *
   * <p>Bốn thao tác sau đây là atomic — tất cả thành công
   * hoặc tất cả rollback:
   * <ol>
   *   <li>Cập nhật {@code current_price} của item
   *       (optimistic lock: {@code current_price < bidAmount})
   *   <li>Hoàn tiền cho bidder cũ nếu có và khác bidder mới
   *   <li>Trừ tiền bidder mới
   *   <li>Lưu bản ghi vào {@code bid_transaction}
   * </ol>
   *
   * <p>Connection được mở, dùng và đóng hoàn toàn trong method này.
   * Không có connection leak.
   *
   * @param bidderId      ID người đặt giá mới
   * @param sessionId        ID sessionId
   * @param bidAmount     số tiền đặt
   * @param prevLeader    thông tin bidder đang dẫn đầu, empty nếu chưa có bid
   * @return {@link BidExecutionResult} chứa thông tin sau khi thành công
   * @throws BidOutpricedException nếu có bid khác vào trước với giá cao hơn
   * @throws InsufficientBalanceException nếu balance thay đổi giữa validate và execute
   * @throws SQLException nếu lỗi DB khác
   */
  public BidExecutionResult executeBid(
      int bidderId,
      int sessionId,
      BigDecimal bidAmount,
      Optional<LeaderInfo> prevLeader)
      throws SQLException {

    // Connection được lấy, dùng và đóng tại đây
    // Không truyền Connection ra ngoài method
    Connection conn = DatabaseConnection.getInstance().getConnection();
    conn.setAutoCommit(false);

    try {
      // -- Bước 1: Atomic price update với optimistic lock
      int affected = updateItemPrice(conn, sessionId, bidAmount);
      if (affected == 0) {
        // affected = 0: có bid khác vào trước, giá đã cao hơn bidAmount
        throw new BidOutpricedException(
            "Đã có người đặt giá cao hơn. Vui lòng thử lại");
      }

      // -- Bước 2: Hoàn tiền bidder cũ
      if (prevLeader.isPresent()) {
        LeaderInfo leader = prevLeader.get();

        if (leader.bidderId() != bidderId) {
          refundBidder(conn, leader.bidderId(), leader.bidAmount(), sessionId);
          logger.debug(
              "Hoàn tiền bidderId={}, amount={}",
              leader.bidderId(), leader.bidAmount());
        }
      }

      // -- Bước 3: Trừ tiền bidder mới
      boolean holdBalance = holdBalance(conn, bidderId, bidAmount, sessionId);
      if (!holdBalance) {
        // Balance thay đổi giữa validate và execute — race condition hiếm
        throw new InsufficientBalanceException(
            "Số dư thay đổi trong quá trình xử lý. Vui lòng thử lại");
      }

      // -- Bước 4: Lưu bid_transaction -- dùng đúng tên cột bid_amount
      long transactionId = insertBidTransaction(conn, bidderId, sessionId, bidAmount);

      // -- Bước 5: Đếm tổng bid trong cùng transaction
      int totalBids = countByItemInTx(conn, sessionId);

      conn.commit();

      logger.info(
          "executeBid thành công: txId={}, sessionId={}, bidderId={}, price={}",
          transactionId, sessionId, bidderId, bidAmount);

      return new BidExecutionResult(transactionId, totalBids);

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
        "SELECT COUNT(*) FROM bid_transaction WHERE item_id = ?";

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
          SELECT bt.bid_id, bt.session_id, bt.bidder_id, u.username, bt.bid_amount, bt.created_at
          FROM bid_transaction bt
          JOIN users u ON bt.bidder_id = u.user_id
          WHERE session_id = ?
          ORDER BY bt.created_at DESC
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
        ORDER BY bt.created_at DESC;
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
   * Cập nhật giá item.
   * Điều kiện {@code current_price < bidAmount} là optimistic lock.
   *
   * @return số row affected -- 0 nếu có bid khác vào trước
   */
  private int updateItemPrice(Connection conn, int sessionId, BigDecimal bidAmount)
      throws SQLException {
    String sql =
        "UPDATE auction_session"
            + " SET current_price = ?,"
            + "     bid_count = bid_count + 1"
            + " WHERE session_id = ?"
            + "   AND current_price < ?"
            + "   AND status = 'ACTIVE'"
            + "   AND is_deleted = false";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setBigDecimal(1, bidAmount);
      ps.setInt(2, sessionId);
      ps.setBigDecimal(3, bidAmount);
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
  private void refundBidder(Connection conn, int bidderId, BigDecimal amount, int sessionId)
      throws SQLException {
    String sql = 
        "UPDATE users "
            + "SET balance = balance + ?, "
            + "    frozen_balance = frozen_balance - ? "
            + "WHERE user_id = ? "
            + "  AND is_deleted = false";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setBigDecimal(1, amount);
      ps.setBigDecimal(2, amount);
      ps.setInt(3, bidderId);

      int affectedRows = ps.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Không thể hoàn tiền: Người dùng không tồn tại hoặc đã bị xóa.");
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
  private boolean holdBalance(Connection conn, int bidderId, BigDecimal amount, int sessionId)
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

  /**
   * Lưu bid_transaction -- dùng đúng tên cột {@code bid_amount} theo schema.
   *
   * @return generated key (bid_transaction_id)
   */
  private long insertBidTransaction(
      Connection conn, int bidderId, int sessionId, BigDecimal bidAmount)
      throws SQLException {
    String sql =
        "INSERT INTO bid_transaction"
            + " (bidder_id, session_id, bid_amount, created_at)"
            + " VALUES (?, ?, ?, ?)";

    try (PreparedStatement ps =
        conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, bidderId);
      ps.setInt(2, sessionId);
      ps.setBigDecimal(3, bidAmount);  // tên cột: bid_amount
      ps.setTimestamp(4, Timestamp.from(Instant.now()));
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          return keys.getLong(1);
        }
      }
    }
    throw new SQLException(
        "INSERT bid_transaction thành công nhưng không có generated key");
  }

  /** Đếm bid trong cùng transaction -- tránh đọc stale data. */
  private int countByItemInTx(Connection conn, int sessionId) throws SQLException {
    String sql = "SELECT COUNT(*) FROM bid_transaction WHERE item_id = ?";
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