package vn.edu.vnu.uet.group8.server.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.vnu.uet.group8.common.dto.model.CommentDTO;
import vn.edu.vnu.uet.group8.common.dto.model.ReviewDTO;
import vn.edu.vnu.uet.group8.common.dto.model.TransactionHistoryEntry;
import vn.edu.vnu.uet.group8.common.dto.model.TransactionRecord;
import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;
import vn.edu.vnu.uet.group8.common.enums.TransactionStatus;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;

@ExtendWith(MockitoExtension.class)
class SmallDaoTest {

  @Mock private DatabaseConnection dbConn;
  @Mock private Connection conn;
  @Mock private PreparedStatement ps;
  @Mock private ResultSet rs;

  private MockedStatic<DatabaseConnection> staticMock;

  @BeforeEach
  void setUp() throws SQLException {
    staticMock = mockStatic(DatabaseConnection.class);
    staticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
    lenient().when(dbConn.getConnection()).thenReturn(conn);
  }

  @AfterEach
  void tearDown() {
    staticMock.close();
  }

  // ═══════════════════════════════════════════════════════════════
  // RatingDAO
  // ═══════════════════════════════════════════════════════════════
  @Nested
  @DisplayName("RatingDAO")
  class RatingDAOTest {

    private RatingDAO dao;

    @BeforeEach
    void init() {
      dao = new RatingDAO();
    }

    @Test
    @DisplayName("insertRating - gọi đúng tham số")
    void insertRating() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);

      dao.insertRating(1, "user1", 2, 10, 5, "Rất tốt");

      verify(ps).setInt(1, 1);
      verify(ps).setString(2, "user1");
      verify(ps).setInt(3, 2);
      verify(ps).setInt(4, 10);
      verify(ps).setInt(5, 5);
      verify(ps).setString(6, "Rất tốt");
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("insertRating - comment null")
    void insertRatingCommentNull() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      dao.insertRating(1, "u", 2, 10, 4, null);
      verify(ps).setInt(1, 1);
      verify(ps).setString(2, "u");
      verify(ps).setInt(3, 2);
      verify(ps).setInt(4, 10);
      verify(ps).setInt(5, 4);
      verify(ps).setString(6, null);
    }

    @Test
    @DisplayName("insertRating - SQLException → ném lên")
    void insertRatingSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertThrows(SQLException.class, () -> dao.insertRating(1, "u", 2, 10, 3, ""));
    }

    @Test
    @DisplayName("hasRated - true khi đã đánh giá")
    void hasRatedTrue() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);

      assertTrue(dao.hasRated(1, 2));
      verify(ps).setInt(1, 1);
      verify(ps).setInt(2, 2);
    }

    @Test
    @DisplayName("hasRated - false khi chưa đánh giá")
    void hasRatedFalse() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertFalse(dao.hasRated(1, 99));
    }

    @Test
    @DisplayName("hasBoughtFrom - true khi đã mua")
    void hasBoughtFromTrue() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);

      assertTrue(dao.hasBoughtFrom(3, 5));
      verify(ps).setInt(1, 3);
      verify(ps).setInt(2, 5);
    }

    @Test
    @DisplayName("hasBoughtFrom - false khi chưa mua")
    void hasBoughtFromFalse() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertFalse(dao.hasBoughtFrom(1, 2));
    }

    @Test
    @DisplayName("getSellerReviews - trả về danh sách đúng")
    void getSellerReviews() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      Instant now = Instant.now();
      when(rs.next()).thenReturn(true, true, false);
      when(rs.getString("username")).thenReturn("u1", "u2");
      when(rs.getInt("score")).thenReturn(5, 4);
      when(rs.getString("comment")).thenReturn("Xuất sắc", "Ổn");
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));

      List<ReviewDTO> reviews = dao.getSellerReviews(10);

      assertEquals(2, reviews.size());
      assertEquals("u1", reviews.get(0).getRaterUsername());
      assertEquals(5, reviews.get(0).getScore());
      verify(ps).setInt(1, 10);
    }

    @Test
    @DisplayName("getSellerReviews - created_at null → null trong DTO")
    void getSellerReviewsNullDate() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, false);
      when(rs.getString("username")).thenReturn("u");
      when(rs.getInt("score")).thenReturn(3);
      when(rs.getString("comment")).thenReturn("ok");
      when(rs.getTimestamp("created_at")).thenReturn(null);

      List<ReviewDTO> reviews = dao.getSellerReviews(1);

      assertEquals(1, reviews.size());
      assertNull(reviews.get(0).getCreatedAt());
    }

    @Test
    @DisplayName("getSellerReviews - list rỗng khi không có review")
    void getSellerReviewsEmpty() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.getSellerReviews(99).isEmpty());
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // FavoriteDAO
  // ═══════════════════════════════════════════════════════════════
  @Nested
  @DisplayName("FavoriteDAO")
  class FavoriteDAOTest {

    private FavoriteDAO dao;

    @BeforeEach
    void init() {
      dao = new FavoriteDAO();
    }

    @Test
    @DisplayName("addFavorite - gọi đúng tham số")
    void addFavorite() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      dao.addFavorite(3, 10);
      verify(ps).setInt(1, 3);
      verify(ps).setInt(2, 10);
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("addFavorite - SQLException → ném lên")
    void addFavoriteSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertThrows(SQLException.class, () -> dao.addFavorite(1, 1));
    }

    @Test
    @DisplayName("removeFavorite - gọi đúng tham số")
    void removeFavorite() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      dao.removeFavorite(5, 20);
      verify(ps).setInt(1, 5);
      verify(ps).setInt(2, 20);
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("removeFavorite - SQLException → ném lên")
    void removeFavoriteSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertThrows(SQLException.class, () -> dao.removeFavorite(1, 1));
    }

    @Test
    @DisplayName("getFavoriteItemIds - trả về danh sách item_id")
    void getFavoriteItemIds() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true, true, true, false);
      when(rs.getInt("item_id")).thenReturn(10, 20, 30);

      List<Integer> result = dao.getFavoriteItemIds(1);

      assertEquals(3, result.size());
      assertEquals(10, result.get(0));
      assertEquals(30, result.get(2));
      verify(ps).setInt(1, 1);
    }

    @Test
    @DisplayName("getFavoriteItemIds - list rỗng")
    void getFavoriteItemIdsEmpty() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.getFavoriteItemIds(99).isEmpty());
    }

    @Test
    @DisplayName("getFavoriteItemIds - SQLException → ném lên")
    void getFavoriteItemIdsSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertThrows(SQLException.class, () -> dao.getFavoriteItemIds(1));
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // AutoBidDAO
  // ═══════════════════════════════════════════════════════════════
  @Nested
  @DisplayName("AutoBidDAO")
  class AutoBidDAOTest {

    private AutoBidDAO dao;
    @Mock private PreparedStatement psDeactivate;
    @Mock private PreparedStatement psInsert;
    @Mock private ResultSet keyRs;

    @BeforeEach
    void init() {
      dao = new AutoBidDAO();
    }

    @Test
    @DisplayName("saveConfig - deactivate cũ rồi insert mới thành công")
    void saveConfig() throws SQLException {
      when(conn.prepareStatement(contains("UPDATE auto_bid_config"))).thenReturn(psDeactivate);
      when(conn.prepareStatement(
              contains("INSERT INTO auto_bid_config"), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(psInsert);

      AutoBidConfig config = AutoBidConfig.builder()
          .sessionId(5).userId(3).maxPrice(new BigDecimal("500000")).build();

      dao.saveConfig(config);

      verify(conn).setAutoCommit(false);
      verify(psDeactivate).setInt(1, 5);
      verify(psDeactivate).setInt(2, 3);
      verify(psDeactivate).executeUpdate();
      verify(psInsert).setInt(1, 5);
      verify(psInsert).setInt(2, 3);
      verify(psInsert).setBigDecimal(3, new BigDecimal("500000"));
      verify(psInsert).setBoolean(4, true);
      verify(conn).commit();
      verify(conn).setAutoCommit(true);
    }

    @Test
    @DisplayName("saveConfig - SQLException → rollback + ném lên")
    void saveConfigRollback() throws SQLException {
      when(conn.prepareStatement(contains("UPDATE auto_bid_config"))).thenReturn(psDeactivate);
      when(conn.prepareStatement(contains("INSERT"), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenThrow(new SQLException("insert lỗi"));

      AutoBidConfig config = AutoBidConfig.builder()
          .sessionId(1).userId(1).maxPrice(BigDecimal.TEN).build();

      assertThrows(SQLException.class, () -> dao.saveConfig(config));
      verify(conn).rollback();
    }

    @Test
    @DisplayName("deactivate - gọi đúng tham số")
    void deactivate() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      dao.deactivate(7);
      verify(ps).setInt(1, 7);
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("deactivate - SQLException → ném lên")
    void deactivateSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertThrows(SQLException.class, () -> dao.deactivate(1));
    }

    @Test
    @DisplayName("findActiveBySession - trả về configs đúng")
    void findActiveBySession() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      Instant now = Instant.now();
      when(rs.next()).thenReturn(true, false);
      when(rs.getInt("config_id")).thenReturn(1);
      when(rs.getInt("session_id")).thenReturn(5);
      when(rs.getInt("user_id")).thenReturn(3);
      when(rs.getBigDecimal("max_price")).thenReturn(new BigDecimal("500000"));
      when(rs.getBoolean("is_active")).thenReturn(true);
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));

      List<AutoBidConfig> configs = dao.findActiveBySession(5);

      assertEquals(1, configs.size());
      assertEquals(5, configs.get(0).getSessionId());
      assertEquals(3, configs.get(0).getUserId());
      assertTrue(configs.get(0).isActive());
      verify(ps).setInt(1, 5);
    }

    @Test
    @DisplayName("findActiveBySession - list rỗng khi không có config")
    void findActiveBySessionEmpty() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.findActiveBySession(99).isEmpty());
    }

    @Test
    @DisplayName("findActiveBySession - SQLException → ném lên")
    void findActiveBySessionSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertThrows(SQLException.class, () -> dao.findActiveBySession(1));
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // CommentDAO
  // ═══════════════════════════════════════════════════════════════
  @Nested
  @DisplayName("CommentDAO")
  class CommentDAOTest {

    private CommentDAO dao;

    @BeforeEach
    void init() {
      dao = new CommentDAO();
    }

    @Test
    @DisplayName("insert - gọi đúng tham số, không ném exception")
    void insert() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenReturn(ps);
      dao.insert(10, 3, "user_a", "Sản phẩm tốt lắm!");
      verify(ps).setInt(1, 10);
      verify(ps).setInt(2, 3);
      verify(ps).setString(3, "user_a");
      verify(ps).setString(4, "Sản phẩm tốt lắm!");
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("insert - SQLException bị catch + log, không ném lên")
    void insertSQLExceptionBiCatch() throws SQLException {
      when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
          .thenThrow(new SQLException("DB lỗi"));
      // Không ném exception - CommentDAO.insert() catch internally
      assertDoesNotThrow(() -> dao.insert(1, 1, "u", "c"));
    }

    @Test
    @DisplayName("getCommentsForAnItemId - trả về danh sách đúng")
    void getCommentsForAnItemId() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      Instant now = Instant.now();
      when(rs.next()).thenReturn(true, true, false);
      when(rs.getString("username")).thenReturn("u1", "u2");
      when(rs.getInt("user_id")).thenReturn(1, 2);
      when(rs.getString("content")).thenReturn("Hay lắm", "Bình thường");
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));

      List<CommentDTO> result = dao.getCommentsForAnItemId(10);

      assertEquals(2, result.size());
      assertEquals("u1", result.get(0).getUsername());
      assertEquals("Hay lắm", result.get(0).getContent());
      verify(ps).setInt(1, 10);
    }

    @Test
    @DisplayName("getCommentsForAnItemId - list rỗng khi không có comment")
    void getCommentsForAnItemIdEmpty() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.getCommentsForAnItemId(99).isEmpty());
    }

    @Test
    @DisplayName("getCommentsForAnItemId - SQLException bị catch → list rỗng")
    void getCommentsForAnItemIdSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      List<CommentDTO> result = dao.getCommentsForAnItemId(1);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getCommentsForUser - trả về danh sách đúng")
    void getCommentsForUser() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      Instant now = Instant.now();
      when(rs.next()).thenReturn(true, false);
      when(rs.getString("username")).thenReturn("seller01");
      when(rs.getInt("user_id")).thenReturn(5);
      when(rs.getString("content")).thenReturn("Giao hàng nhanh");
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));

      List<CommentDTO> result = dao.getCommentsForUser(5);

      assertEquals(1, result.size());
      assertEquals("seller01", result.get(0).getUsername());
      verify(ps).setInt(1, 5);
    }

    @Test
    @DisplayName("getCommentsForUser - list rỗng khi không có comment")
    void getCommentsForUserEmpty() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.getCommentsForUser(99).isEmpty());
    }

    @Test
    @DisplayName("getCommentsForUser - SQLException bị catch → list rỗng")
    void getCommentsForUserSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      assertTrue(dao.getCommentsForUser(1).isEmpty());
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // TransactionDAO
  // ═══════════════════════════════════════════════════════════════
  @Nested
  @DisplayName("TransactionDAO")
  class TransactionDAOTest {

    private TransactionDAO dao;

    @BeforeEach
    void init() {
      dao = new TransactionDAO();
    }

    @Test
    @DisplayName("insertTransaction - gọi đúng tham số, sessionId not null")
    void insertTransactionWithSession() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);

      dao.insertTransaction(
          conn,
          "tx-001",
          5,
          new BigDecimal("100000"),
          TransactionType.DEPOSIT,
          TransactionStatus.SUCCESS,
          "Nạp tiền",
          10);

      verify(ps).setString(1, "tx-001");
      verify(ps).setInt(2, 5);
      verify(ps).setBigDecimal(3, new BigDecimal("100000"));
      verify(ps).setString(4, "DEPOSIT");
      verify(ps).setString(5, "SUCCESS");
      verify(ps).setString(6, "Nạp tiền");
      verify(ps).setInt(7, 10);
      verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("insertTransaction - sessionId null → setInt(7, 0)")
    void insertTransactionNullSession() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      dao.insertTransaction(
          conn,
          "tx-002",
          5,
          new BigDecimal("50000"),
          TransactionType.WITHDRAW,
          TransactionStatus.SUCCESS,
          "Rút tiền",
          null);
      verify(ps).setInt(7, 0);
    }

    @Test
    @DisplayName("insertTransaction - status null → default SUCCESS")
    void insertTransactionNullStatus() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      dao.insertTransaction(
          conn, "tx-003", 1, BigDecimal.TEN, TransactionType.BID_HOLD, null, "hold", null);
      verify(ps).setString(5, "SUCCESS");
    }

    @Test
    @DisplayName("insertTransaction - description null → empty string")
    void insertTransactionNullDescription() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      dao.insertTransaction(
          conn,
          "tx-004",
          1,
          BigDecimal.TEN,
          TransactionType.BID_REFUND,
          TransactionStatus.SUCCESS,
          null,
          null);
      verify(ps).setString(6, "");
    }

    @Test
    @DisplayName("getTransactionsByUserId - map đúng TransactionHistoryEntry")
    void getTransactionsByUserId() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      Instant now = Instant.now();
      when(rs.next()).thenReturn(true, true, false);
      when(rs.getString("transaction_id")).thenReturn("tx-1", "tx-2");
      when(rs.getBigDecimal("amount")).thenReturn(new BigDecimal("100000"), new BigDecimal("50000"));
      when(rs.getString("transaction_type")).thenReturn("DEPOSIT", "WITHDRAW");
      when(rs.getString("description")).thenReturn("Nạp tiền", "Rút tiền");
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));

      List<TransactionHistoryEntry> result = dao.getTransactionsByUserId(3);

      assertEquals(2, result.size());
      assertEquals("tx-1", result.get(0).transactionId());
      assertEquals(TransactionType.DEPOSIT, result.get(0).type());
      verify(ps).setInt(1, 3);
    }

    @Test
    @DisplayName("getTransactionsByUserId - list rỗng")
    void getTransactionsByUserIdEmpty() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertTrue(dao.getTransactionsByUserId(99).isEmpty());
    }

    @Test
    @DisplayName("getTransactionsByUserId - unknown type → null type trong entry")
    void getTransactionsByUserIdUnknownType() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      Instant now = Instant.now();
      when(rs.next()).thenReturn(true, false);
      when(rs.getString("transaction_id")).thenReturn("tx-x");
      when(rs.getBigDecimal("amount")).thenReturn(BigDecimal.ONE);
      when(rs.getString("transaction_type")).thenReturn("UNKNOWN_TYPE");
      when(rs.getString("description")).thenReturn("?");
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));

      List<TransactionHistoryEntry> result = dao.getTransactionsByUserId(1);

      assertEquals(1, result.size());
      assertNull(result.get(0).type()); // parseToTransactionType returns null on unknown
    }

    @Test
    @DisplayName("getTransactionsBySessionId - map đúng TransactionRecord")
    void getTransactionsBySessionId() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      Instant now = Instant.now();
      when(rs.next()).thenReturn(true, false);
      when(rs.getString("transaction_id")).thenReturn("tx-10");
      when(rs.getInt("user_id")).thenReturn(3);
      when(rs.getBigDecimal("amount")).thenReturn(new BigDecimal("200000"));
      when(rs.getString("transaction_type")).thenReturn("BID_HOLD");
      when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(now));
      when(rs.getString("status")).thenReturn("SUCCESS");
      when(rs.getString("description")).thenReturn("Ký quỹ");
      when(rs.getInt("session_id")).thenReturn(5);

      List<TransactionRecord> result = dao.getTransactionsBySessionId(5);

      assertEquals(1, result.size());
      assertEquals("tx-10", result.get(0).transactionId());
      assertEquals(TransactionType.BID_HOLD, result.get(0).transactionType());
      verify(ps).setInt(1, 5);
    }

    @Test
    @DisplayName("getTransactionsBySessionId - SQLException bị catch → list rỗng")
    void getTransactionsBySessionIdSQLException() throws SQLException {
      when(conn.prepareStatement(anyString())).thenThrow(new SQLException("DB"));
      List<TransactionRecord> result = dao.getTransactionsBySessionId(1);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("existsTransaction(String) - true khi tồn tại")
    void existsTransactionTrue() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getBoolean(1)).thenReturn(true);

      assertTrue(dao.existsTransaction("tx-abc"));
      verify(ps).setString(1, "tx-abc");
    }

    @Test
    @DisplayName("existsTransaction(String) - false khi không tồn tại")
    void existsTransactionFalse() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getBoolean(1)).thenReturn(false);

      assertFalse(dao.existsTransaction("tx-xyz"));
    }

    @Test
    @DisplayName("existsTransaction(Connection, String) - true")
    void existsTransactionWithConnTrue() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(true);
      when(rs.getBoolean(1)).thenReturn(true);

      assertTrue(dao.existsTransaction(conn, "tx-conn-001"));
    }

    @Test
    @DisplayName("existsTransaction(Connection, String) - false")
    void existsTransactionWithConnFalse() throws SQLException {
      when(conn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);
      when(rs.next()).thenReturn(false);

      assertFalse(dao.existsTransaction(conn, "tx-not-exist"));
    }
  }
}