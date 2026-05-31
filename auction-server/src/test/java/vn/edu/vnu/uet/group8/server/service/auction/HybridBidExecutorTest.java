package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;

@ExtendWith(MockitoExtension.class)
class HybridBidExecutorTest {

  @Mock private BidTransactionDAO bidTransactionDAO;
  @Mock private AutoBidDAO autoBidDAO;
  @Mock private AuctionSessionDAO sessionDAO;
  @Mock private DatabaseConnection dbConn;
  @Mock private Connection conn;

  private HybridBidExecutor executor;
  private MockedStatic<DatabaseConnection> databaseStaticMock;

  @BeforeEach
  void setUp() throws SQLException {
    databaseStaticMock = mockStatic(DatabaseConnection.class);
    databaseStaticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
    lenient().when(dbConn.getConnection()).thenReturn(conn);

    executor = new HybridBidExecutor(bidTransactionDAO, autoBidDAO, sessionDAO);
  }

  @AfterEach
  void tearDown() {
    databaseStaticMock.close();
  }

  private BidContext createContext(int challengerId, int sessionId, int itemId, BigDecimal bidAmount, int bidCount, BigDecimal startingPrice, BigDecimal currentPrice, Integer highestBidderId) {
    User bidder = UserMember.reconstructor()
        .id(challengerId)
        .createdAt(Instant.now())
        .isDeleted(false)
        .username("challenger_user")
        .email("test@mail.com")
        .encryptedPassword("hash")
        .status(UserStatus.ACTIVE)
        .roles(Set.of(UserRole.BIDDER))
        .balance(BigDecimal.ZERO)
        .phone("123456789")
        .build();

    Item item = Item.reconstructor()
        .id(itemId)
        .createdAt(Instant.now())
        .isDeleted(false)
        .sellerId(99)
        .title("Sản phẩm test")
        .category(ItemCategory.ELECTRONICS)
        .status(ItemStatus.LISTED)
        .build();

    AuctionSession session = AuctionSession.reconstructor()
        .id(sessionId)
        .createdAt(Instant.now())
        .isDeleted(false)
        .itemId(itemId)
        .startingPrice(startingPrice)
        .currentPrice(currentPrice)
        .status(SessionStatus.ACTIVE)
        .startTime(Instant.now().minusSeconds(100))
        .endTime(Instant.now().plusSeconds(3600))
        .bidCount(bidCount)
        .highestBidderId(highestBidderId)
        .build();

    return new BidContext(bidder, item, session, bidAmount, false);
  }

  @Test
  @DisplayName("execute() - Thất bại khi phiên đấu giá không tồn tại")
  void testSessionNotFound() throws SQLException {
    BidContext context = createContext(1, 101, 201, new BigDecimal("1500"), 0, new BigDecimal("1000"), new BigDecimal("1000"), null);
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.empty());

    assertThrows(ValidationException.class, () -> executor.execute(context, false));
    verify(conn).rollback();
  }

  @Test
  @DisplayName("execute() - Thất bại khi giá đặt thấp hơn giá tối thiểu")
  void testBidAmountBelowMinimum() throws SQLException {
    // Session has 1 bid, current price is 1000. Step = max(1000 * 0.01, 1000) = 1000. Min next bid = 2000.
    // We place a bid of 1500.
    BidContext context = createContext(1, 101, 201, new BigDecimal("1500"), 1, new BigDecimal("1000"), new BigDecimal("1000"), 2);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));

    assertThrows(ValidationException.class, () -> executor.execute(context, false));
    verify(conn).rollback();
  }

  @Test
  @DisplayName("execute() - Thất bại khi không đủ số dư để đóng băng balance")
  void testHoldBalanceFailed() throws SQLException {
    BidContext context = createContext(1, 101, 201, new BigDecimal("2000"), 0, new BigDecimal("1000"), new BigDecimal("1000"), null);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));
    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(Collections.emptyList());
    when(bidTransactionDAO.holdBalance(conn, 1, new BigDecimal("2000"), 101)).thenReturn(false);

    assertThrows(ValidationException.class, () -> executor.execute(context, false));
    verify(conn).rollback();
  }

  @Test
  @DisplayName("execute() - Thất bại khi không thể cập nhật thông tin session")
  void testUpdateSessionFailed() throws SQLException {
    BidContext context = createContext(1, 101, 201, new BigDecimal("2000"), 0, new BigDecimal("1000"), new BigDecimal("1000"), null);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));
    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(Collections.emptyList());
    when(bidTransactionDAO.holdBalance(conn, 1, new BigDecimal("2000"), 101)).thenReturn(true);
    when(bidTransactionDAO.updateSessionAfterFight(conn, 101, new BigDecimal("2000"), 1, 1)).thenReturn(0);

    assertThrows(ValidationException.class, () -> executor.execute(context, false));
    verify(conn).rollback();
  }

  @Test
  @DisplayName("execute() - Đấu giá thủ công thành công (giá vọt lên trần)")
  void testManualBidSuccess() throws SQLException {
    BidContext context = createContext(1, 101, 201, new BigDecimal("3000"), 0, new BigDecimal("1000"), new BigDecimal("1000"), null);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));
    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(Collections.emptyList());
    when(bidTransactionDAO.holdBalance(conn, 1, new BigDecimal("3000"), 101)).thenReturn(true);
    when(bidTransactionDAO.insertBidTransaction(conn, 1, 101, new BigDecimal("3000"), "LEADER")).thenReturn(123L);
    when(bidTransactionDAO.updateSessionAfterFight(conn, 101, new BigDecimal("3000"), 1, 1)).thenReturn(1);
    when(bidTransactionDAO.countByItemInTx(conn, 101)).thenReturn(1);

    BidResult result = executor.execute(context, false);

    assertNotNull(result);
    assertEquals(123L, result.getTransactionId());
    assertEquals(new BigDecimal("3000"), result.getNewPrice());
    assertEquals(1, result.getTotalBids());
    verify(conn).commit();
  }

  @Test
  @DisplayName("execute() - Đấu giá tự động thành công (không có đối thủ, phiên mới)")
  void testAutoBidNoOpponentNewSession() throws SQLException {
    BidContext context = createContext(1, 101, 201, new BigDecimal("3000"), 0, new BigDecimal("1000"), new BigDecimal("1000"), null);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));
    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(Collections.emptyList());
    when(bidTransactionDAO.holdBalance(conn, 1, new BigDecimal("3000"), 101)).thenReturn(true);
    when(bidTransactionDAO.insertBidTransaction(conn, 1, 101, new BigDecimal("1000"), "LEADER")).thenReturn(124L);
    when(bidTransactionDAO.updateSessionAfterFight(conn, 101, new BigDecimal("1000"), 1, 1)).thenReturn(1);
    when(bidTransactionDAO.countByItemInTx(conn, 101)).thenReturn(1);

    BidResult result = executor.execute(context, true);

    assertNotNull(result);
    // Should be starting price because it's first bid with no opponent
    assertEquals(new BigDecimal("1000"), result.getNewPrice());
    verify(autoBidDAO).saveConfig(eq(conn), any(AutoBidConfig.class));
    verify(conn).commit();
  }

  @Test
  @DisplayName("execute() - Đấu giá tự động thành công (không có đối thủ, giữ nguyên giá của chính mình)")
  void testAutoBidNoOpponentSameBidder() throws SQLException {
    BidContext context = createContext(1, 101, 201, new BigDecimal("3000"), 2, new BigDecimal("1000"), new BigDecimal("1200"), 1);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));
    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(Collections.emptyList());
    when(bidTransactionDAO.holdBalance(conn, 1, new BigDecimal("3000"), 101)).thenReturn(true);
    when(bidTransactionDAO.insertBidTransaction(conn, 1, 101, new BigDecimal("1200"), "LEADER")).thenReturn(125L);
    when(bidTransactionDAO.updateSessionAfterFight(conn, 101, new BigDecimal("1200"), 1, 1)).thenReturn(1);

    BidResult result = executor.execute(context, true);

    assertNotNull(result);
    // Should keep currentPrice (1200) since same bidder updates autobid
    assertEquals(new BigDecimal("1200"), result.getNewPrice());
    verify(conn).commit();
  }

  @Test
  @DisplayName("execute() - Đấu giá tự động bằng điểm tối đa với đối thủ (Huề điểm -> Top 1 thắng)")
  void testAutoBidWithOpponentTie() throws SQLException {
    BidContext context = createContext(1, 101, 201, new BigDecimal("2000"), 1, new BigDecimal("1000"), new BigDecimal("1000"), 2);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));

    AutoBidConfig opponentConfig = AutoBidConfig.builder()
        .userId(2)
        .sessionId(101)
        .maxPrice(new BigDecimal("2000"))
        .build();
    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(List.of(opponentConfig));
    when(bidTransactionDAO.holdBalance(conn, 2, new BigDecimal("2000"), 101)).thenReturn(true);
    when(bidTransactionDAO.updateSessionAfterFight(conn, 101, new BigDecimal("2000"), 2, 2)).thenReturn(1);

    BidResult result = executor.execute(context, true);

    assertNotNull(result);
    // Both capped at 2000, so newPrice is 2000
    assertEquals(new BigDecimal("2000"), result.getNewPrice());
    verify(bidTransactionDAO).insertBidTransaction(conn, 1, 101, new BigDecimal("2000"), "OUTBID");
    verify(autoBidDAO).deactivateOutbidConfigs(conn, 101, new BigDecimal("2000"), 2);
  }

  @Test
  @DisplayName("execute() - Đấu giá tự động với đối thủ (defenseTarget vượt quá maxPrice của Top 1)")
  void testAutoBidWithOpponentDefenseExceedsMaxPrice() throws SQLException {
    // Challenger places autobid of 2000. Current price = 1000. Step = 1000.
    // Opponent has config maxPrice of 1950.
    // defenseTarget = 1950 + 1000 = 2950 > 2000 (challenger max).
    // Expected new price = 2000 (capped at challenger max).
    BidContext context = createContext(1, 101, 201, new BigDecimal("2000"), 1, new BigDecimal("1000"), new BigDecimal("1000"), 2);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));

    AutoBidConfig opponentConfig = AutoBidConfig.builder()
        .userId(2)
        .sessionId(101)
        .maxPrice(new BigDecimal("1950"))
        .build();
    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(List.of(opponentConfig));
    when(bidTransactionDAO.holdBalance(conn, 1, new BigDecimal("2000"), 101)).thenReturn(true);
    when(bidTransactionDAO.updateSessionAfterFight(conn, 101, new BigDecimal("2000"), 1, 2)).thenReturn(1);

    BidResult result = executor.execute(context, true);

    assertNotNull(result);
    assertEquals(new BigDecimal("2000"), result.getNewPrice());
  }

  @Test
  @DisplayName("execute() - Đấu giá tự động với đối thủ (defenseTarget nhỏ hơn maxPrice của Top 1)")
  void testAutoBidWithOpponentDefenseWithinMaxPrice() throws SQLException {
    // Challenger places autobid of 2500. Current price = 1000. Step = 1000.
    // Opponent has config maxPrice of 1200.
    // defenseTarget = 1200 + 1000 = 2200 <= 2500.
    // Expected new price = 2200.
    BidContext context = createContext(1, 101, 201, new BigDecimal("2500"), 1, new BigDecimal("1000"), new BigDecimal("1000"), 2);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));

    AutoBidConfig opponentConfig = AutoBidConfig.builder()
        .userId(2)
        .sessionId(101)
        .maxPrice(new BigDecimal("1200"))
        .build();
    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(List.of(opponentConfig));
    when(bidTransactionDAO.holdBalance(conn, 1, new BigDecimal("2500"), 101)).thenReturn(true);
    when(bidTransactionDAO.updateSessionAfterFight(conn, 101, new BigDecimal("2200"), 1, 2)).thenReturn(1);

    BidResult result = executor.execute(context, true);

    assertNotNull(result);
    assertEquals(new BigDecimal("2200"), result.getNewPrice());
  }

  @Test
  @DisplayName("execute() - Hoàn trả tiền của Leader trước đó")
  void testRefundPreviousLeader() throws SQLException {
    BidContext context = createContext(1, 101, 201, new BigDecimal("3000"), 2, new BigDecimal("1000"), new BigDecimal("1500"), 3);
    AuctionSession session = context.getAuctionSession();
    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(session));

    AutoBidConfig previousLeaderConfig = AutoBidConfig.builder()
        .userId(3)
        .sessionId(101)
        .maxPrice(new BigDecimal("2200")) // previous leader config max is 2200
        .build();

    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(List.of(previousLeaderConfig));
    when(bidTransactionDAO.holdBalance(conn, 1, new BigDecimal("3000"), 101)).thenReturn(true);
    when(bidTransactionDAO.updateSessionAfterFight(conn, 101, new BigDecimal("3000"), 1, 2)).thenReturn(1);

    executor.execute(context, false);

    // Verify refund called for previous leader (user 3) with max price (2200)
    verify(bidTransactionDAO).refundBidder(conn, 3, new BigDecimal("2200"), 101);
  }

  @Test
  @DisplayName("execute() - Đấu giá tự động với đối thủ (defenseTarget nhỏ hơn currentPrice -> newPrice = currentPrice)")
  void testAutoBidDefenseTargetBelowCurrentPrice() throws SQLException {
    BidContext context = createContext(1, 101, 201, new BigDecimal("3000"), 1, new BigDecimal("1000"), new BigDecimal("2000"), 2);
    AuctionSession session = context.getAuctionSession();
    // Tạo 1 instance khác để test nhánh sync context
    AuctionSession lockedSession = AuctionSession.reconstructor()
        .id(101)
        .createdAt(Instant.now())
        .isDeleted(false)
        .itemId(201)
        .startingPrice(new BigDecimal("1000"))
        .currentPrice(new BigDecimal("2000"))
        .status(SessionStatus.ACTIVE)
        .startTime(Instant.now().minusSeconds(100))
        .endTime(Instant.now().plusSeconds(3600))
        .bidCount(1)
        .highestBidderId(2)
        .build();

    when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(lockedSession));

    AutoBidConfig opponentConfig = AutoBidConfig.builder()
        .userId(2)
        .sessionId(101)
        .maxPrice(new BigDecimal("1500"))
        .build();
    when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(List.of(opponentConfig));
    when(bidTransactionDAO.holdBalance(conn, 1, new BigDecimal("3000"), 101)).thenReturn(true);
    when(bidTransactionDAO.updateSessionAfterFight(conn, 101, new BigDecimal("3000"), 1, 2)).thenReturn(1);

    BidResult result = executor.execute(context, true);

    assertNotNull(result);
    assertEquals(new BigDecimal("3000"), result.getNewPrice());
    // Kiểm tra sync: context session phải được cập nhật
    assertEquals(new BigDecimal("3000"), context.getAuctionSession().getCurrentPrice());
    assertEquals(1, context.getAuctionSession().getHighestBidderId());
  }
}
