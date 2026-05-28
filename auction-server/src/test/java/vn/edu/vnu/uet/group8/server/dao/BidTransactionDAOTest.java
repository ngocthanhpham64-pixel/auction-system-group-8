package vn.edu.vnu.uet.group8.server.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.BidAction;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.BidExecutionResult;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.BidHistoryEntry;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.BidOutpricedException;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.InsufficientBalanceException;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.LeaderInfo;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.UserBidRecord;

@ExtendWith(MockitoExtension.class)
class BidTransactionDAOTest {

  @Mock private DatabaseConnection dbConn;
  @Mock private Connection conn;
  @Mock private ResultSet rs;

  private BidTransactionDAO dao;
  private MockedStatic<DatabaseConnection> staticMock;

  @BeforeEach
  void setUp() throws SQLException {
    staticMock = mockStatic(DatabaseConnection.class);
    staticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
    when(dbConn.getConnection()).thenReturn(conn);

    dao = new BidTransactionDAO();
  }

  @AfterEach
  void tearDown() {
    staticMock.close();
  }

  @Nested
  @DisplayName("executeBid()")
  class ExecuteBidTest {

    @Test
    @DisplayName("Success - bid đầu tiên")
    void successBidDauTien() throws SQLException {

      PreparedStatement psUpdate = mock(PreparedStatement.class);
      PreparedStatement psHold = mock(PreparedStatement.class);
      PreparedStatement psWalletTx = mock(PreparedStatement.class);
      PreparedStatement psInsertBid = mock(PreparedStatement.class);
      PreparedStatement psCount = mock(PreparedStatement.class);

      when(conn.prepareStatement(contains("UPDATE auction_session"))).thenReturn(psUpdate);

      when(conn.prepareStatement(contains("frozen_balance +"))).thenReturn(psHold);

      when(conn.prepareStatement(contains("INSERT INTO wallet_transaction"))).thenReturn(psWalletTx);

      when(conn.prepareStatement(
              contains("INSERT INTO bid_transaction"), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
          .thenReturn(psInsertBid);

      when(conn.prepareStatement(contains("COUNT(*)"))).thenReturn(psCount);

      when(psUpdate.executeUpdate()).thenReturn(1);
      when(psHold.executeUpdate()).thenReturn(1);
      when(psInsertBid.executeUpdate()).thenReturn(1);

      ResultSet keySet = mock(ResultSet.class);
      when(psInsertBid.getGeneratedKeys()).thenReturn(keySet);
      when(keySet.next()).thenReturn(true);
      when(keySet.getLong(1)).thenReturn(99L);

      ResultSet countSet = mock(ResultSet.class);
      when(psCount.executeQuery()).thenReturn(countSet);
      when(countSet.next()).thenReturn(true);
      when(countSet.getInt(1)).thenReturn(1);

      BidExecutionResult result =
          dao.executeFightBatch(
              1,
              new BigDecimal("1500000"),
              5,
              List.of(new BidAction(5, new BigDecimal("1500000"))),
              true,
              null,
              null,
              new BigDecimal("1500000"),
              1);

      assertEquals(99L, result.transactionId());
      assertEquals(1, result.totalBids());

      verify(conn).commit();
    }

    @Test
    @DisplayName("Success - có refund bidder cũ")
    void successCoRefund() throws SQLException {

      PreparedStatement psUpdate = mock(PreparedStatement.class);
      PreparedStatement psRefund = mock(PreparedStatement.class);
      PreparedStatement psHold = mock(PreparedStatement.class);
      PreparedStatement psWalletTx = mock(PreparedStatement.class);
      PreparedStatement psInsertBid = mock(PreparedStatement.class);
      PreparedStatement psCount = mock(PreparedStatement.class);

      when(conn.prepareStatement(contains("UPDATE auction_session"))).thenReturn(psUpdate);

      when(conn.prepareStatement(contains("balance +"))).thenReturn(psRefund);

      when(conn.prepareStatement(contains("frozen_balance +"))).thenReturn(psHold);

      when(conn.prepareStatement(contains("INSERT INTO wallet_transaction"))).thenReturn(psWalletTx);

      when(conn.prepareStatement(
              contains("INSERT INTO bid_transaction"), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
          .thenReturn(psInsertBid);

      when(conn.prepareStatement(contains("COUNT(*)"))).thenReturn(psCount);

      when(psUpdate.executeUpdate()).thenReturn(1);
      when(psRefund.executeUpdate()).thenReturn(1);
      when(psHold.executeUpdate()).thenReturn(1);
      when(psInsertBid.executeUpdate()).thenReturn(1);

      ResultSet keySet = mock(ResultSet.class);
      when(psInsertBid.getGeneratedKeys()).thenReturn(keySet);
      when(keySet.next()).thenReturn(true);
      when(keySet.getLong(1)).thenReturn(10L);

      ResultSet countSet = mock(ResultSet.class);
      when(psCount.executeQuery()).thenReturn(countSet);
      when(countSet.next()).thenReturn(true);
      when(countSet.getInt(1)).thenReturn(3);

      LeaderInfo prevLeader = new LeaderInfo(7, new BigDecimal("1000000"));

      BidExecutionResult result =
          dao.executeFightBatch(
              1,
              new BigDecimal("1500000"),
              5,
              List.of(
                  new BidAction(7, new BigDecimal("1000000")),
                  new BidAction(5, new BigDecimal("1500000"))),
              true,
              7,
              new BigDecimal("1000000"),
              new BigDecimal("1500000"),
              2);

      assertEquals(10L, result.transactionId());
      assertEquals(3, result.totalBids());

      verify(psRefund).executeUpdate();
    }

    @Test
    @DisplayName("Same bidder → không refund")
    void sameBidderNoRefund() throws SQLException {

      PreparedStatement psUpdate = mock(PreparedStatement.class);
      PreparedStatement psInsertBid = mock(PreparedStatement.class);
      PreparedStatement psCount = mock(PreparedStatement.class);

      when(conn.prepareStatement(contains("UPDATE auction_session"))).thenReturn(psUpdate);

      when(conn.prepareStatement(
              contains("INSERT INTO bid_transaction"), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
          .thenReturn(psInsertBid);

      when(conn.prepareStatement(contains("COUNT(*)"))).thenReturn(psCount);

      when(psUpdate.executeUpdate()).thenReturn(1);
      when(psInsertBid.executeUpdate()).thenReturn(1);

      ResultSet keySet = mock(ResultSet.class);
      when(psInsertBid.getGeneratedKeys()).thenReturn(keySet);
      when(keySet.next()).thenReturn(true);
      when(keySet.getLong(1)).thenReturn(20L);

      ResultSet countSet = mock(ResultSet.class);
      when(psCount.executeQuery()).thenReturn(countSet);
      when(countSet.next()).thenReturn(true);
      when(countSet.getInt(1)).thenReturn(2);

      LeaderInfo prevLeader = new LeaderInfo(1, new BigDecimal("1000000"));

      dao.executeFightBatch(
          1,
          new BigDecimal("1500000"),
          1,
          List.of(new BidAction(1, new BigDecimal("1500000"))),
          false,
          1,
          new BigDecimal("1000000"),
          new BigDecimal("1500000"),
          1);

      verify(conn, never()).prepareStatement(contains("balance +"));
    }

    @Test
    @DisplayName("Outpriced → rollback")
    void outpriced() throws SQLException {

      PreparedStatement psUpdate = mock(PreparedStatement.class);
      PreparedStatement psHold = mock(PreparedStatement.class);
      PreparedStatement psWalletTx = mock(PreparedStatement.class);
      PreparedStatement psInsertBid = mock(PreparedStatement.class);

      when(conn.prepareStatement(contains("UPDATE auction_session"))).thenReturn(psUpdate);
      when(conn.prepareStatement(contains("frozen_balance +"))).thenReturn(psHold);
      when(conn.prepareStatement(contains("INSERT INTO wallet_transaction"))).thenReturn(psWalletTx);
      when(conn.prepareStatement(
              contains("INSERT INTO bid_transaction"), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
          .thenReturn(psInsertBid);

      when(psUpdate.executeUpdate()).thenReturn(0);
      when(psHold.executeUpdate()).thenReturn(1);
      when(psInsertBid.executeUpdate()).thenReturn(1);

      ResultSet keySet = mock(ResultSet.class);
      when(psInsertBid.getGeneratedKeys()).thenReturn(keySet);
      when(keySet.next()).thenReturn(true);
      when(keySet.getLong(1)).thenReturn(99L);

      assertThrows(
          BidOutpricedException.class,
          () ->
              dao.executeFightBatch(
                  1,
                  BigDecimal.TEN,
                  5,
                  List.of(new BidAction(5, BigDecimal.TEN)),
                  true,
                  null,
                  null,
                  BigDecimal.TEN,
                  1));

      verify(conn).rollback();
    }

    @Test
    @DisplayName("Insufficient balance → rollback")
    void insufficientBalance() throws SQLException {

      PreparedStatement psHold = mock(PreparedStatement.class);

      when(conn.prepareStatement(contains("frozen_balance +"))).thenReturn(psHold);

      when(psHold.executeUpdate()).thenReturn(0);

      assertThrows(
          InsufficientBalanceException.class,
          () ->
              dao.executeFightBatch(
                  1,
                  new BigDecimal("1000000"),
                  5,
                  List.of(new BidAction(5, new BigDecimal("1000000"))),
                  true,
                  null,
                  null,
                  new BigDecimal("1000000"),
                  1));

      verify(conn).rollback();
    }
  }

  @Nested
  @DisplayName("findCurrentLeader()")
  class FindCurrentLeaderTest {

    @Test
    void coLeader() throws SQLException {

      PreparedStatement ps = mock(PreparedStatement.class);

      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      when(rs.next()).thenReturn(true);
      when(rs.getInt("bidder_id")).thenReturn(7);
      when(rs.getBigDecimal("bid_amount")).thenReturn(new BigDecimal("2000000"));

      Optional<LeaderInfo> result = dao.findCurrentLeader(5);

      assertTrue(result.isPresent());
      assertEquals(7, result.get().bidderId());
    }

    @Test
    void khongCoLeader() throws SQLException {

      PreparedStatement ps = mock(PreparedStatement.class);

      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      when(rs.next()).thenReturn(false);

      assertTrue(dao.findCurrentLeader(5).isEmpty());
    }
  }

  @Nested
  @DisplayName("findHistoryByItem()")
  class FindHistoryByItemTest {

    @Test
    void coHistory() throws SQLException {

      PreparedStatement ps = mock(PreparedStatement.class);

      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      Instant now = Instant.now();

      when(rs.next()).thenReturn(true, false);

      when(rs.getInt("bid_id")).thenReturn(1);
      when(rs.getInt("session_id")).thenReturn(5);
      when(rs.getInt("bidder_id")).thenReturn(7);

      when(rs.getString("username")).thenReturn("user01");

      when(rs.getBigDecimal("bid_amount")).thenReturn(new BigDecimal("2000000"));

      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));

      List<BidHistoryEntry> result = dao.findHistoryByItem(5);

      assertEquals(1, result.size());
      assertEquals("user01", result.get(0).bidderUsername());
    }

    @Test
    void emptyHistory() throws SQLException {

      PreparedStatement ps = mock(PreparedStatement.class);

      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      when(rs.next()).thenReturn(false);

      assertTrue(dao.findHistoryByItem(5).isEmpty());
    }
  }

  @Nested
  @DisplayName("findHistoryByUser()")
  class FindHistoryByUserTest {

    @Test
    void coHistory() throws SQLException {

      PreparedStatement ps = mock(PreparedStatement.class);

      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      Instant now = Instant.now();

      when(rs.next()).thenReturn(true, false);

      when(rs.getInt("bid_id")).thenReturn(10);
      when(rs.getInt("session_id")).thenReturn(5);
      when(rs.getInt("item_id")).thenReturn(20);

      when(rs.getString("item_title")).thenReturn("iPhone 17");

      when(rs.getBigDecimal("bid_amount")).thenReturn(new BigDecimal("2000000"));

      when(rs.getBigDecimal("current_price")).thenReturn(new BigDecimal("2500000"));

      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));

      when(rs.getTimestamp("end_time")).thenReturn(Timestamp.from(now.plusSeconds(3600)));

      List<UserBidRecord> result = dao.findHistoryByUser(3);

      assertEquals(1, result.size());
      assertEquals("iPhone 17", result.get(0).itemTitle());
    }

    @Test
    void emptyHistory() throws SQLException {

      PreparedStatement ps = mock(PreparedStatement.class);

      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      when(rs.next()).thenReturn(false);

      assertTrue(dao.findHistoryByUser(3).isEmpty());
    }
  }
}
