package vn.edu.vnu.uet.group8.server.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

@ExtendWith(MockitoExtension.class)
class AuctionSessionDAOTest {

  @Mock private DatabaseConnection dbConn;
  @Mock private Connection conn;
  @Mock private PreparedStatement ps;
  @Mock private ResultSet rs;
  @Mock private ResultSet keyRs;

  private AuctionSessionDAO dao;
  private MockedStatic<DatabaseConnection> staticMock;

  @BeforeEach
  void setUp() throws SQLException {
    staticMock = mockStatic(DatabaseConnection.class);
    staticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
    lenient().when(dbConn.getConnection()).thenReturn(conn);
    dao = new AuctionSessionDAO();
  }

  @AfterEach
  void tearDown() {
    staticMock.close();
  }

  // Helper: thiết lập mapRow cho ResultSet
  private void setupSessionRs(int sessionId, int itemId, String status) throws SQLException {
    when(rs.getInt("session_id")).thenReturn(sessionId);
    when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
    when(rs.getBoolean("is_deleted")).thenReturn(false);
    when(rs.getInt("item_id")).thenReturn(itemId);
    when(rs.getBigDecimal("starting_price")).thenReturn(new BigDecimal("1000000"));
    when(rs.getBigDecimal("current_price")).thenReturn(new BigDecimal("1500000"));
    when(rs.getString("status")).thenReturn(status);
    when(rs.getTimestamp("start_time"))
        .thenReturn(Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));
    when(rs.getTimestamp("end_time"))
        .thenReturn(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));
    when(rs.getInt("bid_count")).thenReturn(3);
    when(rs.getInt("highest_bidder_id")).thenReturn(7);
  }

  // ─────────────────────────────────────────────────────────────
  // insert
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("insert()")
  class InsertTest {

    @Test
    @DisplayName("insert thành công - gán id từ generated key")
    void insertThanhCong() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(55);

      Instant start = Instant.now();
      Instant end = start.plus(2, ChronoUnit.HOURS);
      AuctionSession session = new AuctionSession.Builder(
          10, new BigDecimal("1000000"), start, end).build();

      dao.insert(session);

      assertEquals(55, session.getId());
      verify(ps).setInt(1, 10);
      verify(ps).setBigDecimal(2, new BigDecimal("1000000"));
    }

    @Test
    @DisplayName("insert - highestBidderId null → setNull")
    void insertHighestBidderNull() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(1);

      Instant now = Instant.now();
      AuctionSession session = new AuctionSession.Builder(
          5, new BigDecimal("500000"), now, now.plus(1, ChronoUnit.HOURS)).build();

      dao.insert(session);

      verify(ps).setNull(eq(9), anyInt());
    }

    @Test
    @DisplayName("insert - không lấy được generated key → SQLException")
    void insertKhongLayDuocKey() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(false);

      Instant now = Instant.now();
      AuctionSession session = new AuctionSession.Builder(
          1, BigDecimal.TEN, now, now.plus(1, ChronoUnit.HOURS)).build();

      assertThrows(SQLException.class, () -> dao.insert(session));
    }

    @Test
    @DisplayName("insert - SQLException từ conn → ném lên")
    void insertSQLException() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenThrow(new SQLException("DB lỗi"));

      Instant now = Instant.now();
      AuctionSession session = new AuctionSession.Builder(
          1, BigDecimal.TEN, now, now.plus(1, ChronoUnit.HOURS)).build();

      assertThrows(SQLException.class, () -> dao.insert(session));
    }

    @Test
    @DisplayName("insert - highestBidderId non-null → setInt")
    void insertHighestBidderNonNull() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      when(ps.getGeneratedKeys()).thenReturn(keyRs);
      when(keyRs.next()).thenReturn(true);
      when(keyRs.getInt(1)).thenReturn(1);

      Instant now = Instant.now();
      AuctionSession session = new AuctionSession.Builder(
          5, new BigDecimal("500000"), now, now.plus(1, ChronoUnit.HOURS))
          .highestBidderId(7)
          .build();

      dao.insert(session);

      verify(ps).setInt(9, 7);
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findById
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findById()")
  class FindByIdTest {

    @Test
    @DisplayName("Tìm thấy → trả Optional.of(session)")
    void timThay() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupSessionRs(5, 10, "ACTIVE");

      Optional<AuctionSession> result = dao.findById(5);

      assertTrue(result.isPresent());
      assertEquals(5, result.get().getId());
      assertEquals(SessionStatus.ACTIVE, result.get().getStatus());
      verify(ps).setInt(1, 5);
    }

    @Test
    @DisplayName("Không tìm thấy → trả Optional.empty()")
    void khongTimThay() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      Optional<AuctionSession> result = dao.findById(999);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("highestBidderId = 0 trong DB → null trong entity")
    void highestBidderIdZero() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupSessionRs(1, 1, "UPCOMING");
      when(rs.getInt("highest_bidder_id")).thenReturn(0); // override

      Optional<AuctionSession> result = dao.findById(1);

      assertTrue(result.isPresent());
      assertNull(result.get().getHighestBidderId());
    }

    @Test
    @DisplayName("SQLException → ném lên")
    void sqlException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));

      assertThrows(SQLException.class, () -> dao.findById(1));
    }

    @Test
    @DisplayName("start_time và end_time là null → ném IllegalStateException do thiếu field bắt buộc")
    void startEndTimeNull() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);

      setupSessionRs(1, 10, "ACTIVE");
      when(rs.getTimestamp("start_time")).thenReturn(null);
      when(rs.getTimestamp("end_time")).thenReturn(null);

      assertThrows(IllegalStateException.class, () -> dao.findById(1));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findActiveSessionByItemId
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findActiveSessionByItemId()")
  class FindActiveByItemTest {

    @Test
    @DisplayName("Có session ACTIVE → trả Optional.of")
    void timThay() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupSessionRs(3, 10, "ACTIVE");

      Optional<AuctionSession> result = dao.findActiveSessionByItemId(10);

      assertTrue(result.isPresent());
      verify(ps).setInt(1, 10);
    }

    @Test
    @DisplayName("Không có session ACTIVE → Optional.empty()")
    void khongTimThay() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.findActiveSessionByItemId(99).isEmpty());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findUpcomingByItemId
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findUpcomingByItemId()")
  class FindUpcomingByItemTest {

    @Test
    @DisplayName("Có session UPCOMING → trả Optional.of")
    void timThay() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupSessionRs(7, 5, "UPCOMING");

      Optional<AuctionSession> result = dao.findUpcomingByItemId(5);

      assertTrue(result.isPresent());
      assertEquals(7, result.get().getId());
    }

    @Test
    @DisplayName("Không có session UPCOMING → Optional.empty()")
    void khongTimThay() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.findUpcomingByItemId(1).isEmpty());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findByItemId (queryList)
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findByItemId()")
  class FindByItemIdTest {

    @Test
    @DisplayName("Có nhiều session → trả list đầy đủ")
    void nhieuSession() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, true, false);

      setupSessionRs(1, 5, "ACTIVE");
      // Second row
      when(rs.getInt("session_id")).thenReturn(1, 2);

      List<AuctionSession> result = dao.findByItemId(5);

      assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Không có session → list rỗng")
    void listRong() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.findByItemId(99).isEmpty());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // updateCurrentBid
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("updateCurrentBid()")
  class UpdateCurrentBidTest {

    @Test
    @DisplayName("Update thành công - 1 row bị ảnh hưởng")
    void updateThanhCong() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(1);

      assertDoesNotThrow(() -> dao.updateCurrentBid(3, 7, new BigDecimal("2000000")));

      verify(ps).setBigDecimal(1, new BigDecimal("2000000"));
      verify(ps).setInt(2, 7);
      verify(ps).setInt(3, 3);
      verify(ps).setBigDecimal(4, new BigDecimal("2000000"));
    }

    @Test
    @DisplayName("0 rows bị ảnh hưởng (giá cũ >= giá mới) → IllegalStateException")
    void zeroRowsAffected() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeUpdate()).thenReturn(0);

      assertThrows(
          IllegalStateException.class,
          () -> dao.updateCurrentBid(3, 7, new BigDecimal("500000")));
    }

    @Test
    @DisplayName("SQLException → ném lên")
    void sqlException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));

      assertThrows(
          SQLException.class,
          () -> dao.updateCurrentBid(1, 1, BigDecimal.TEN));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // updateStatus
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("updateStatus()")
  class UpdateStatusTest {

    @Test
    @DisplayName("updateStatus ACTIVE → SOLD thành công")
    void updateStatusThanhCong() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);

      dao.updateStatus(5, SessionStatus.SOLD);

      verify(ps).setString(1, "SOLD");
      verify(ps).setInt(2, 5);
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("updateStatus tất cả SessionStatus")
    void updateAllStatus() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      for (SessionStatus s : SessionStatus.values()) {
        dao.updateStatus(1, s);
        verify(ps, atLeastOnce()).setString(1, s.name());
      }
    }

    @Test
    @DisplayName("SQLException → ném lên")
    void sqlException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertThrows(SQLException.class, () -> dao.updateStatus(1, SessionStatus.CANCELLED));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // updateEndTime
  // ─────────────────────────────────────────────────────────────
  @Test
  @DisplayName("updateEndTime() gọi đúng SQL")
  void updateEndTime() throws SQLException {
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    Instant newEnd = Instant.now().plus(30, ChronoUnit.MINUTES);

    dao.updateEndTime(3, newEnd);

    verify(ps).setTimestamp(eq(1), any(Timestamp.class));
    verify(ps).setInt(2, 3);
    verify(ps).executeUpdate();
  }

  // ─────────────────────────────────────────────────────────────
  // softDelete
  // ─────────────────────────────────────────────────────────────
  @Test
  @DisplayName("softDelete() gọi đúng SQL")
  void softDelete() throws SQLException {
    when(conn.prepareStatement(anyString())).thenReturn(ps);

    dao.softDelete(9);

    verify(ps).setInt(1, 9);
    verify(ps).executeUpdate();
  }

  // ─────────────────────────────────────────────────────────────
  // findAllActive
  // ─────────────────────────────────────────────────────────────
  @Test
  @DisplayName("findAllActive() - trả list đúng")
  void findAllActive() throws SQLException {
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    setupSessionRs(1, 2, "ACTIVE");

    List<AuctionSession> result = dao.findAllActive();

    assertEquals(1, result.size());
    assertEquals(SessionStatus.ACTIVE, result.get(0).getStatus());
  }

  // ─────────────────────────────────────────────────────────────
  // findExpiredActive
  // ─────────────────────────────────────────────────────────────
  @Test
  @DisplayName("findExpiredActive() - list rỗng khi không có session hết hạn")
  void findExpiredActiveEmpty() throws SQLException {
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    assertTrue(dao.findExpiredActive().isEmpty());
    verify(ps).setTimestamp(eq(1), any(Timestamp.class));
  }

  // ─────────────────────────────────────────────────────────────
  // findInSnipingWindow
  // ─────────────────────────────────────────────────────────────
  @Test
  @DisplayName("findInSnipingWindow(5) - gọi đúng SQL với 2 timestamps")
  void findInSnipingWindow() throws SQLException {
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    dao.findInSnipingWindow(5);

    verify(ps).setTimestamp(eq(1), any(Timestamp.class));
    verify(ps).setTimestamp(eq(2), any(Timestamp.class));
  }

  // ─────────────────────────────────────────────────────────────
  // findWonSessionsByUserId
  // ─────────────────────────────────────────────────────────────
  @Test
  @DisplayName("findWonSessionsByUserId() - trả danh sách phiên đã thắng")
  void findWonSessions() throws SQLException {
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    setupSessionRs(10, 5, "SOLD");

    List<AuctionSession> result = dao.findWonSessionsByUserId(7);

    assertEquals(1, result.size());
    verify(ps).setInt(1, 7);
  }

  // ─────────────────────────────────────────────────────────────
  // lockSessionForUpdate
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("lockSessionForUpdate()")
  class LockSessionForUpdateTest {

    @Test
    @DisplayName("Tìm thấy và lock thành công → trả Optional.of")
    void lockThanhCong() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      setupSessionRs(100, 200, "ACTIVE");

      Optional<AuctionSession> result = dao.lockSessionForUpdate(conn, 100);

      assertTrue(result.isPresent());
      assertEquals(100, result.get().getId());
      verify(ps).setInt(1, 100);
    }

    @Test
    @DisplayName("Không tìm thấy để lock → trả Optional.empty")
    void lockKhongTimThay() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      Optional<AuctionSession> result = dao.lockSessionForUpdate(conn, 999);

      assertTrue(result.isEmpty());
      verify(ps).setInt(1, 999);
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findActiveByCategory
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findActiveByCategory()")
  class FindActiveByCategoryTest {

    @Test
    @DisplayName("Có session ACTIVE theo category → trả list đúng")
    void coSessionActiveTheoCategory() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      setupSessionRs(1, 10, "ACTIVE");

      List<AuctionSession> result = dao.findActiveByCategory(vn.edu.vnu.uet.group8.common.enums.ItemCategory.ELECTRONICS);

      assertEquals(1, result.size());
      verify(ps).setString(1, vn.edu.vnu.uet.group8.common.enums.ItemCategory.ELECTRONICS.name());
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findUpcomingToStart
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findUpcomingToStart()")
  class FindUpcomingToStartTest {

    @Test
    @DisplayName("Có session UPCOMING đến giờ bắt đầu → trả list đúng")
    void coSessionUpcomingToStart() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      setupSessionRs(2, 20, "UPCOMING");

      List<AuctionSession> result = dao.findUpcomingToStart();

      assertEquals(1, result.size());
      verify(ps).setTimestamp(eq(1), any(Timestamp.class));
    }
  }

  // ─────────────────────────────────────────────────────────────
  // findByPriceRange
  // ─────────────────────────────────────────────────────────────
  @Nested
  @DisplayName("findByPriceRange()")
  class FindByPriceRangeTest {

    @Test
    @DisplayName("Không truyền filter và sort → dùng mặc định")
    void khongTruyenGi() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      setupSessionRs(1, 10, "ACTIVE");

      List<AuctionSession> result = dao.findByPriceRange(null, null, null, null);

      assertEquals(1, result.size());
      verify(ps, never()).setString(anyInt(), anyString());
      verify(ps, never()).setBigDecimal(anyInt(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Có category, minPrice, maxPrice, và SortOption.NEWEST")
    void coFilterVaSortNewest() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      setupSessionRs(1, 10, "ACTIVE");

      List<AuctionSession> result = dao.findByPriceRange(
          vn.edu.vnu.uet.group8.common.enums.ItemCategory.ELECTRONICS,
          new BigDecimal("100"),
          new BigDecimal("500"),
          vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest.SortOption.NEWEST
      );

      assertEquals(1, result.size());
      verify(ps).setString(1, vn.edu.vnu.uet.group8.common.enums.ItemCategory.ELECTRONICS.name());
      verify(ps).setBigDecimal(2, new BigDecimal("100"));
      verify(ps).setBigDecimal(3, new BigDecimal("500"));
    }

    @Test
    @DisplayName("SortOption.ENDING_SOON")
    void sortEndingSoon() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      dao.findByPriceRange(null, null, null, vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest.SortOption.ENDING_SOON);

      verify(conn).prepareStatement(contains("ORDER BY s.end_time ASC"));
    }

    @Test
    @DisplayName("SortOption.PRICE_ASC")
    void sortPriceAsc() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      dao.findByPriceRange(null, null, null, vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest.SortOption.PRICE_ASC);

      verify(conn).prepareStatement(contains("ORDER BY s.current_price ASC"));
    }

    @Test
    @DisplayName("SortOption.PRICE_DESC")
    void sortPriceDesc() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      dao.findByPriceRange(null, null, null, vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest.SortOption.PRICE_DESC);

      verify(conn).prepareStatement(contains("ORDER BY s.current_price DESC"));
    }

    @Test
    @DisplayName("SortOption.HOT")
    void sortHot() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      dao.findByPriceRange(null, null, null, vn.edu.vnu.uet.group8.common.dto.request.GetAuctionsRequest.SortOption.HOT);

      verify(conn).prepareStatement(contains("ORDER BY s.bid_count DESC, s.current_price DESC"));
    }
  }
}