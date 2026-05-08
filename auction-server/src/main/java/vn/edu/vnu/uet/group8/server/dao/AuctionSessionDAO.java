package vn.edu.vnu.uet.group8.server.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;

public class AuctionSessionDAO {

  private static final Gson GSON = new Gson();

  private Connection getConn() throws SQLException {
    return DatabaseConnection.getInstance().getConnection();
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 1 — mapRow: ResultSet → AuctionSession
  // ═══════════════════════════════════════════════════

  private AuctionSession mapRow(ResultSet rs) throws SQLException {
    return AuctionSession.reconstructor()
        .id(rs.getInt("session_id"))
        .createdAt(rs.getTimestamp("created_at").toInstant())
        .isDeleted(rs.getBoolean("is_deleted"))
        .itemId(rs.getInt("item_id"))
        .startingPrice(rs.getBigDecimal("starting_price"))
        .currentPrice(rs.getBigDecimal("current_price"))
        .status(ItemStatus.valueOf(rs.getString("status")))
        .startTime(toInstant(rs.getTimestamp("start_time")))
        .endTime(toInstant(rs.getTimestamp("end_time")))
        .bidCount(rs.getInt("bid_count"))
        .highestBidderId(rs.getInt("highest_bidder_id") == 0 ? null : rs.getInt("highest_bidder_id"))
        .build();
  }

  // ═══════════════════════════════════════════════════
  // PHẦN 2 — CRUD CƠ BẢN
  // ═══════════════════════════════════════════════════

  public void insert(AuctionSession session) throws SQLException {
    String sql = """
        INSERT INTO auction_session
          (item_id, starting_price, current_price, status, start_time, end_time, created_at, is_deleted, highest_bidder_id)
        VALUES (?,?,?,?,?,?,?,?,?)
        """;

    try (PreparedStatement ps = getConn().prepareStatement(
            sql, Statement.RETURN_GENERATED_KEYS)) {

      ps.setInt(1, session.getItemId());
      ps.setBigDecimal(2, session.getStartingPrice());
      ps.setBigDecimal(3, session.getCurrentPrice());
      ps.setString(4, session.getStatus().name());
      ps.setTimestamp(5, Timestamp.from(session.getStartTime()));
      ps.setTimestamp(6, Timestamp.from(session.getEndTime()));
      ps.setTimestamp(7, Timestamp.from(session.getCreatedAt()));
      ps.setBoolean(8, session.isDeleted());

      if (session.getHighestBidderId() != null) {
        ps.setInt(9, session.getHighestBidderId());
      } else {
        ps.setNull(9, Types.INTEGER);
      }

      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
          session.assignId(keys.getInt(1));
        } else {
          throw new SQLException("INSERT thành công nhưng không lấy được generated key.");
        }
      }
    }
  }

  public Optional<AuctionSession> findById(int sessionId) throws SQLException {
    String sql = """
        SELECT * FROM auction_session
        WHERE session_id = ? AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return Optional.of(mapRow(rs));
      }
    }
    return Optional.empty();
  }

  public List<AuctionSession> findByItemId(int itemId) throws SQLException {
    String sql = """
        SELECT * FROM auction_session
        WHERE item_id = ? AND is_deleted = false
        ORDER BY created_at DESC
        """;

    return queryList(sql, ps -> ps.setInt(1, itemId));
  }


  public Optional<AuctionSession> findActiveSessionByItemId(int itemId) throws SQLException {
    String sql = """
        SELECT * FROM auction_session
        WHERE item_id = ? 
          AND status = 'ACTIVE' 
          AND is_deleted = false
        """;
    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return Optional.of(mapRow(rs));
      }
    }
    return Optional.empty();
}

  // ═══════════════════════════════════════════════════
  // QUERIES
  // ═══════════════════════════════════════════════════

  public List<AuctionSession> findActiveByCategory(ItemCategory category) throws SQLException {
    String sql = """
        SELECT s.* FROM auction_session s
        JOIN item i ON s.item_id = i.item_id
        WHERE i.category = ?
          AND s.status = 'ACTIVE'
          AND s.is_deleted = false
          AND i.is_deleted = false
        ORDER BY s.end_time ASC
        """;

    return queryList(sql, ps -> ps.setString(1, category.name()));
  }

  public List<AuctionSession> findAllActive() throws SQLException {
    String sql = """
        SELECT * FROM auction_session
        WHERE status = 'ACTIVE' AND is_deleted = false
        ORDER BY end_time ASC
        """;

    return queryList(sql, ps -> {});
  }

  public List<AuctionSession> findByPriceRange(ItemCategory category, BigDecimal minPrice, BigDecimal maxPrice) throws SQLException {
    String sql = """
        SELECT s.* FROM auction_session s
        JOIN item i ON s.item_id = i.item_id
        WHERE i.category = ?
          AND s.status = 'ACTIVE'
          AND s.current_price BETWEEN ? AND ?
          AND s.is_deleted = false
          AND i.is_deleted = false
        ORDER BY s.current_price ASC
        """;

    return queryList(sql, ps -> {
      ps.setString(1, category.name());
      ps.setBigDecimal(2, minPrice);
      ps.setBigDecimal(3, maxPrice);
    });
  }

  public List<AuctionSession> findExpiredActive() throws SQLException {
    String sql = """
        SELECT * FROM auction_session
        WHERE status = 'ACTIVE'
          AND end_time <= ?
          AND is_deleted = false
        """;

    return queryList(sql, ps -> ps.setTimestamp(1, Timestamp.from(Instant.now())));
  }

  public List<AuctionSession> findInSnipingWindow(int minutes) throws SQLException {
    Instant windowStart = Instant.now();
    Instant windowEnd = Instant.now().plusSeconds((long) minutes * 60);

    String sql = """
        SELECT * FROM auction_session
        WHERE status = 'ACTIVE'
          AND end_time BETWEEN ? AND ?
          AND is_deleted = false
        """;

    return queryList(sql, ps -> {
      ps.setTimestamp(1, Timestamp.from(windowStart));
      ps.setTimestamp(2, Timestamp.from(windowEnd));
    });
  }

  // ═══════════════════════════════════════════════════
  // UPDATE METHODS
  // ═══════════════════════════════════════════════════

  public void updateCurrentBid(int sessionId, Integer bidderId, BigDecimal newPrice) throws SQLException {
    String sql = """
        UPDATE auction_session
        SET current_price = ?,
            highest_bidder_id = ?,
            bid_count = bid_count + 1
        WHERE session_id = ?
          AND current_price < ?
          AND status = 'ACTIVE'
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setBigDecimal(1, newPrice);
      ps.setInt(2, bidderId);
      ps.setInt(3, sessionId);
      ps.setBigDecimal(4, newPrice);

      int affected = ps.executeUpdate();
      if (affected == 0)
        throw new IllegalStateException("Không thể cập nhật giá cho session.");
    }
  }

  public void updateStatus(int sessionId, ItemStatus newStatus) throws SQLException {
    String sql = """
        UPDATE auction_session
        SET status = ?
        WHERE session_id = ?
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setString(1, newStatus.name());
      ps.setInt(2, sessionId);
      ps.executeUpdate();
    }
  }

  public void updateEndTime(int sessionId, Instant newEndTime) throws SQLException {
    String sql = """
        UPDATE auction_session
        SET end_time = ?
        WHERE session_id = ?
          AND status = 'ACTIVE'
          AND is_deleted = false
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.from(newEndTime));
      ps.setInt(2, sessionId);
      ps.executeUpdate();
    }
  }

  public void softDelete(int sessionId) throws SQLException {
    String sql = """
        UPDATE auction_session
        SET is_deleted = true
        WHERE session_id = ?
        """;

    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      ps.setInt(1, sessionId);
      ps.executeUpdate();
    }
  }

  // ═══════════════════════════════════════════════════
  // PRIVATE HELPERS
  // ═══════════════════════════════════════════════════

  private List<AuctionSession> queryList(String sql, SqlConsumer<PreparedStatement> binder) throws SQLException {
    List<AuctionSession> result = new ArrayList<>();
    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
      binder.accept(ps);
      try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) result.add(mapRow(rs));
      }
    }
    return result;
  }

  private Instant toInstant(Timestamp ts) {
      return ts != null ? ts.toInstant() : null;
  }

  @FunctionalInterface
  private interface SqlConsumer<T> {
      void accept(T t) throws SQLException;
  }
}