package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.auction.event.AuctionEndedEvent;
import vn.edu.vnu.uet.group8.server.service.auction.event.AuctionOpenedEvent;
import vn.edu.vnu.uet.group8.server.service.user.BalanceService;

@ExtendWith(MockitoExtension.class)
class AuctionClosingServiceTest {

  @Mock private AuctionSessionDAO sessionDAO;
  @Mock private ItemDAO itemDAO;
  @Mock private UserDAO userDAO;
  @Mock private BidTransactionDAO bidDAO;
  @Mock private AutoBidDAO autoBidDAO;
  @Mock private BalanceService balanceService;
  @Mock private AuctionEventBus eventBus;
  @Mock private DatabaseConnection dbConn;
  @Mock private Connection conn;

  private ConcurrentHashMap<Integer, ReentrantLock> sessionLocks;
  private AuctionClosingService service;
  private ScheduledExecutorService mockScheduler;
  private ScheduledFuture<?> mockFuture;
  private Runnable schedulerTask;
  private MockedStatic<DatabaseConnection> databaseStaticMock;

  @BeforeEach
  void setUp() throws SQLException {
    sessionLocks = new ConcurrentHashMap<>();
    mockFuture = mock(ScheduledFuture.class);
    mockScheduler = mock(ScheduledExecutorService.class);

    // Mock DatabaseConnection
    databaseStaticMock = mockStatic(DatabaseConnection.class);
    databaseStaticMock.when(DatabaseConnection::getInstance).thenReturn(dbConn);
    lenient().when(dbConn.getConnection()).thenReturn(conn);

    // Mock Executors static method to inject our mockScheduler
    try (MockedStatic<Executors> executorsMock = mockStatic(Executors.class)) {
      executorsMock.when(() -> Executors.newSingleThreadScheduledExecutor(any()))
          .thenReturn(mockScheduler);

      service = new AuctionClosingService(
          sessionDAO,
          itemDAO,
          userDAO,
          bidDAO,
          autoBidDAO,
          balanceService,
          eventBus,
          sessionLocks
      );
    }

    // Capture the scheduled task when start() is called
    lenient().when(mockScheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
        .thenAnswer(invocation -> {
          schedulerTask = invocation.getArgument(0);
          return mockFuture;
        });
  }

  @AfterEach
  void tearDown() {
    databaseStaticMock.close();
  }

  @Nested
  @DisplayName("Lifecycle (start/stop) Tests")
  class LifecycleTests {

    @Test
    @DisplayName("start() đăng ký scheduled task thành công")
    void testStart() {
      service.start();
      verify(mockScheduler).scheduleAtFixedRate(any(Runnable.class), eq(10L), eq(10L), eq(TimeUnit.SECONDS));
      assertNotNull(schedulerTask);
    }

    @Test
    @DisplayName("stop() hủy task và shutdown scheduler")
    void testStop() {
      service.start();
      service.stop();
      verify(mockFuture).cancel(false);
      verify(mockScheduler).shutdown();
    }
  }

  @Nested
  @DisplayName("Scheduler Core Tests")
  class SchedulerCoreTests {

    @BeforeEach
    void setUpLifecycle() {
      service.start();
    }

    @Test
    @DisplayName("scanAndCloseExpired() xử lý khi xảy ra lỗi SQLException")
    void testScanAndCloseExpiredException() throws SQLException {
      when(sessionDAO.findExpiredActive()).thenThrow(new SQLException("Query error"));

      assertDoesNotThrow(() -> schedulerTask.run());

      verify(sessionDAO, never()).findUpcomingToStart();
    }

    @Test
    @DisplayName("scanAndCloseExpired() không có phiên nào cần xử lý")
    void testNoExpiredOrUpcomingSessions() throws SQLException {
      when(sessionDAO.findExpiredActive()).thenReturn(Collections.emptyList());
      when(sessionDAO.findUpcomingToStart()).thenReturn(Collections.emptyList());

      assertDoesNotThrow(() -> schedulerTask.run());

      verifyNoInteractions(eventBus);
    }

    @Test
    @DisplayName("openSession() thành công")
    void testOpenSessionSuccess() throws SQLException {
      AuctionSession upcomingSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("1000"))
          .status(SessionStatus.UPCOMING)
          .startTime(Instant.now().minusSeconds(10))
          .endTime(Instant.now().plusSeconds(3600))
          .bidCount(0)
          .highestBidderId(null)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(Collections.emptyList());
      when(sessionDAO.findUpcomingToStart()).thenReturn(List.of(upcomingSession));

      schedulerTask.run();

      verify(sessionDAO).updateStatus(101, SessionStatus.ACTIVE);
      verify(itemDAO).updateStatus(201, ItemStatus.LISTED);
      verify(eventBus).publish(any(AuctionOpenedEvent.class));
      assertEquals(SessionStatus.ACTIVE, upcomingSession.getStatus());
    }

    @Test
    @DisplayName("openSession() thất bại khi transitionStatus không hợp lệ")
    void testOpenSessionTransitionFailed() throws SQLException {
      // Session status is already ACTIVE, so UPCOMING -> ACTIVE is invalid
      AuctionSession upcomingSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("1000"))
          .status(SessionStatus.ACTIVE)
          .startTime(Instant.now().minusSeconds(10))
          .endTime(Instant.now().plusSeconds(3600))
          .bidCount(0)
          .highestBidderId(null)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(Collections.emptyList());
      when(sessionDAO.findUpcomingToStart()).thenReturn(List.of(upcomingSession));

      schedulerTask.run();

      verify(sessionDAO, never()).updateStatus(anyInt(), any(SessionStatus.class));
      verify(eventBus, never()).publish(any(AuctionOpenedEvent.class));
    }

    @Test
    @DisplayName("closeSession() - Phiên đấu giá không có lượt đặt giá nào (ENDED_NO_BID)")
    void testCloseSessionEndedNoBid() throws SQLException {
      AuctionSession expiredSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("1000"))
          .status(SessionStatus.ACTIVE)
          .startTime(Instant.now().minusSeconds(3600))
          .endTime(Instant.now().minusSeconds(10))
          .bidCount(0)
          .highestBidderId(null)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(List.of(expiredSession));
      when(sessionDAO.findUpcomingToStart()).thenReturn(Collections.emptyList());
      when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(expiredSession));

      Item item = Item.reconstructor()
          .id(201)
          .createdAt(Instant.now())
          .isDeleted(false)
          .sellerId(301)
          .title("Sách cổ")
          .category(ItemCategory.BOOKS)
          .status(ItemStatus.LISTED)
          .build();
      when(itemDAO.findById(201)).thenReturn(Optional.of(item));

      schedulerTask.run();

      verify(sessionDAO).updateStatus(conn, 101, SessionStatus.ENDED_NO_BID);
      verify(itemDAO).updateStatus(conn, 201, ItemStatus.UNSOLD);
      verify(conn).commit();
      verify(eventBus).publish(any(AuctionEndedEvent.class));
      assertEquals(SessionStatus.ENDED_NO_BID, expiredSession.getStatus());
    }

    @Test
    @DisplayName("closeSession() - Phiên đấu giá có người chiến thắng (SOLD) không có auto-bid tối đa")
    void testCloseSessionSoldWithoutAutoBid() throws SQLException {
      AuctionSession expiredSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("2000"))
          .status(SessionStatus.ACTIVE)
          .startTime(Instant.now().minusSeconds(3600))
          .endTime(Instant.now().minusSeconds(10))
          .bidCount(5)
          .highestBidderId(401)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(List.of(expiredSession));
      when(sessionDAO.findUpcomingToStart()).thenReturn(Collections.emptyList());
      when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(expiredSession));
      when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(Collections.emptyList());

      Item item = Item.reconstructor()
          .id(201)
          .createdAt(Instant.now())
          .isDeleted(false)
          .sellerId(301)
          .title("Máy ảnh")
          .category(ItemCategory.ELECTRONICS)
          .status(ItemStatus.LISTED)
          .build();
      when(itemDAO.findById(201)).thenReturn(Optional.of(item));

      User winner = UserMember.reconstructor()
          .id(401)
          .createdAt(Instant.now())
          .isDeleted(false)
          .username("winner_user")
          .email("test@mail.com")
          .encryptedPassword("hash")
          .status(UserStatus.ACTIVE)
          .roles(Set.of(UserRole.BIDDER))
          .balance(BigDecimal.ZERO)
          .phone("123456789")
          .build();
      when(userDAO.findById(401)).thenReturn(Optional.of(winner));

      schedulerTask.run();

      // Held amount should be the final current price (2000)
      verify(userDAO).settleAuctionPayment(
          eq(conn), anyString(), anyString(), anyString(),
          eq(401), eq(301), eq(101), eq(new BigDecimal("2000")), eq(new BigDecimal("2000"))
      );
      verify(sessionDAO).updateStatus(conn, 101, SessionStatus.SOLD);
      verify(itemDAO).updateStatus(conn, 201, ItemStatus.SOLD);
      verify(conn).commit();
      verify(eventBus).publish(any(AuctionEndedEvent.class));
      assertEquals(SessionStatus.SOLD, expiredSession.getStatus());
    }

    @Test
    @DisplayName("closeSession() - Phiên đấu giá có người chiến thắng (SOLD) và có config auto-bid cao hơn")
    void testCloseSessionSoldWithAutoBid() throws SQLException {
      AuctionSession expiredSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("2000"))
          .status(SessionStatus.ACTIVE)
          .startTime(Instant.now().minusSeconds(3600))
          .endTime(Instant.now().minusSeconds(10))
          .bidCount(5)
          .highestBidderId(401)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(List.of(expiredSession));
      when(sessionDAO.findUpcomingToStart()).thenReturn(Collections.emptyList());
      when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(expiredSession));

      AutoBidConfig activeConfig = AutoBidConfig.builder()
          .userId(401)
          .sessionId(101)
          .maxPrice(new BigDecimal("5000"))
          .build();
      when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(List.of(activeConfig));

      Item item = Item.reconstructor()
          .id(201)
          .createdAt(Instant.now())
          .isDeleted(false)
          .sellerId(301)
          .title("Đồng hồ vàng")
          .category(ItemCategory.WATCHES)
          .status(ItemStatus.LISTED)
          .build();
      when(itemDAO.findById(201)).thenReturn(Optional.of(item));

      User winner = UserMember.reconstructor()
          .id(401)
          .createdAt(Instant.now())
          .isDeleted(false)
          .username("winner_user")
          .email("test@mail.com")
          .encryptedPassword("hash")
          .status(UserStatus.ACTIVE)
          .roles(Set.of(UserRole.BIDDER))
          .balance(BigDecimal.ZERO)
          .phone("123456789")
          .build();
      when(userDAO.findById(401)).thenReturn(Optional.of(winner));

      schedulerTask.run();

      // Held amount should be the maxPrice from AutoBidConfig (5000)
      verify(userDAO).settleAuctionPayment(
          eq(conn), anyString(), anyString(), anyString(),
          eq(401), eq(301), eq(101), eq(new BigDecimal("5000")), eq(new BigDecimal("2000"))
      );
      verify(conn).commit();
      verify(eventBus).publish(any(AuctionEndedEvent.class));
    }

    @Test
    @DisplayName("closeSession() - Lỗi khi lock dòng DB hoặc không tìm thấy session")
    void testCloseSessionLockFailed() throws SQLException {
      AuctionSession expiredSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("2000"))
          .status(SessionStatus.ACTIVE)
          .startTime(Instant.now().minusSeconds(3600))
          .endTime(Instant.now().minusSeconds(10))
          .bidCount(5)
          .highestBidderId(401)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(List.of(expiredSession));
      when(sessionDAO.findUpcomingToStart()).thenReturn(Collections.emptyList());
      when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.empty());

      // Should handle exception gracefully without propagating it
      assertDoesNotThrow(() -> schedulerTask.run());

      verify(conn, never()).commit();
    }

    @Test
    @DisplayName("closeSession() - Lỗi khi thực hiện transaction → rollback")
    void testCloseSessionTransactionRollback() throws SQLException {
      AuctionSession expiredSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("2000"))
          .status(SessionStatus.ACTIVE)
          .startTime(Instant.now().minusSeconds(3600))
          .endTime(Instant.now().minusSeconds(10))
          .bidCount(5)
          .highestBidderId(401)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(List.of(expiredSession));
      when(sessionDAO.findUpcomingToStart()).thenReturn(Collections.emptyList());
      when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(expiredSession));
      when(itemDAO.findById(201)).thenThrow(new RuntimeException("DB Connection Broken"));

      schedulerTask.run();

      verify(conn).rollback();
      verify(conn, never()).commit();
    }

    @Test
    @DisplayName("closeSession() - Session lock thành công nhưng trạng thái không phải ACTIVE")
    void testCloseSessionNotActive() throws SQLException {
      AuctionSession expiredSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("2000"))
          .status(SessionStatus.ACTIVE)
          .startTime(Instant.now().minusSeconds(3600))
          .endTime(Instant.now().minusSeconds(10))
          .bidCount(5)
          .highestBidderId(401)
          .build();

      AuctionSession lockedSessionNonActive = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("2000"))
          .status(SessionStatus.SOLD) // ALREADY SOLD
          .startTime(Instant.now().minusSeconds(3600))
          .endTime(Instant.now().minusSeconds(10))
          .bidCount(5)
          .highestBidderId(401)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(List.of(expiredSession));
      when(sessionDAO.findUpcomingToStart()).thenReturn(Collections.emptyList());
      when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(lockedSessionNonActive));

      schedulerTask.run();

      verify(conn, never()).commit();
      verify(itemDAO, never()).updateStatus(any(), anyInt(), any());
    }
    
    @Test
    @DisplayName("openSessionSafely() - Lỗi khi thực hiện openSession")
    void testOpenSessionSafelyException() throws SQLException {
      AuctionSession upcomingSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("1000"))
          .status(SessionStatus.UPCOMING)
          .startTime(Instant.now().minusSeconds(10))
          .endTime(Instant.now().plusSeconds(3600))
          .bidCount(0)
          .highestBidderId(null)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(Collections.emptyList());
      when(sessionDAO.findUpcomingToStart()).thenReturn(List.of(upcomingSession));
      doThrow(new RuntimeException("Test Exception in OpenSession")).when(sessionDAO).updateStatus(anyInt(), any(SessionStatus.class));

      assertDoesNotThrow(() -> schedulerTask.run());
    }

    @Test
    @DisplayName("closeSession() - Lỗi khi publish Event SOLD vẫn tiếp tục")
    void testCloseSessionPublishSoldEventException() throws SQLException {
      AuctionSession expiredSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("2000"))
          .status(SessionStatus.ACTIVE)
          .startTime(Instant.now().minusSeconds(3600))
          .endTime(Instant.now().minusSeconds(10))
          .bidCount(5)
          .highestBidderId(401)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(List.of(expiredSession));
      when(sessionDAO.findUpcomingToStart()).thenReturn(Collections.emptyList());
      when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(expiredSession));
      when(autoBidDAO.findActiveBySession(conn, 101)).thenReturn(Collections.emptyList());

      Item item = Item.reconstructor()
          .id(201)
          .createdAt(Instant.now())
          .isDeleted(false)
          .sellerId(301)
          .title("Máy ảnh")
          .category(ItemCategory.ELECTRONICS)
          .status(ItemStatus.LISTED)
          .build();
      when(itemDAO.findById(201)).thenReturn(Optional.of(item));

      User winner = UserMember.reconstructor()
          .id(401)
          .createdAt(Instant.now())
          .isDeleted(false)
          .username("winner_user")
          .email("test@mail.com")
          .encryptedPassword("hash")
          .status(UserStatus.ACTIVE)
          .roles(Set.of(UserRole.BIDDER))
          .balance(BigDecimal.ZERO)
          .phone("123456789")
          .build();
      when(userDAO.findById(401)).thenReturn(Optional.of(winner));

      doThrow(new RuntimeException("Event Publish Error")).when(eventBus).publish(any(AuctionEndedEvent.class));

      assertDoesNotThrow(() -> schedulerTask.run());
      verify(conn).commit(); // Vẫn commit DB thành công
    }
    
    @Test
    @DisplayName("closeSession() - Lỗi khi publish Event NO_BID vẫn tiếp tục")
    void testCloseSessionPublishNoBidEventException() throws SQLException {
      AuctionSession expiredSession = AuctionSession.reconstructor()
          .id(101)
          .createdAt(Instant.now())
          .isDeleted(false)
          .itemId(201)
          .startingPrice(new BigDecimal("1000"))
          .currentPrice(new BigDecimal("1000"))
          .status(SessionStatus.ACTIVE)
          .startTime(Instant.now().minusSeconds(3600))
          .endTime(Instant.now().minusSeconds(10))
          .bidCount(0)
          .highestBidderId(null)
          .build();

      when(sessionDAO.findExpiredActive()).thenReturn(List.of(expiredSession));
      when(sessionDAO.findUpcomingToStart()).thenReturn(Collections.emptyList());
      when(sessionDAO.lockSessionForUpdate(conn, 101)).thenReturn(Optional.of(expiredSession));
      
      when(itemDAO.findById(201)).thenReturn(Optional.empty()); // Check Optional.empty() for sellerId fallback

      doThrow(new RuntimeException("Event Publish Error")).when(eventBus).publish(any(AuctionEndedEvent.class));

      assertDoesNotThrow(() -> schedulerTask.run());
      verify(conn).commit(); // Vẫn commit DB thành công
    }
  }
}
