package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.EventType;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.LeaderInfo;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.service.auction.event.AuctionEndedEvent;
import vn.edu.vnu.uet.group8.server.service.user.BalanceService;

import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;
import vn.edu.vnu.uet.group8.server.dao.DatabaseConnection;
import java.sql.Connection;
import java.util.UUID;

/**
 * Scheduler tự động đóng các phiên đấu giá đã hết giờ.
 *
 * <p>Chạy trong thread riêng biệt, không block luồng
 * {@code placeBid()}. Mỗi 30 giây quét một lần, xử lý tuần tự
 * từng phiên hết giờ, gửi broadcast kết quả.
 *
 * <p><b>Luồng xử lý mỗi phiên:</b>
 * <ol>
 *   <li>Tìm tất cả {@code AuctionSession} ACTIVE đã hết giờ
 *   <li>Với mỗi session: tìm người dẫn đầu từ {@code bid_transaction}
 *   <li>Có winner → chuyển SOLD + chuyển tiền cho seller
 *   <li>Không có bid → chuyển ENDED_NO_BID
 *   <li>Broadcast kết quả đến tất cả client
 * </ol>
 *
 * <p>Mỗi phiên được xử lý độc lập trong try-catch —
 * lỗi một phiên không ảnh hưởng các phiên còn lại.
 */
public class AuctionClosingService {

  private static final Logger logger =
      LoggerFactory.getLogger(AuctionClosingService.class);

  // Khoảng thời gian giữa các lần quét (giây)
  private static final long SCAN_INTERVAL_SECONDS = 10;

  // Delay trước lần quét đầu tiên sau khi server khởi động
  private static final long INITIAL_DELAY_SECONDS = 10;

  private final AuctionSessionDAO sessionDAO;
  private final ItemDAO itemDAO;
  private final UserDAO userDAO;
  private final BidTransactionDAO bidDAO;
  private final AutoBidDAO autoBidDAO;
  private final BalanceService balanceService;
  private final AuctionEventBus eventBus;

  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread t = new Thread(runnable, "auction-closing-scheduler");
        t.setDaemon(true);
        return t;
      });

  // Tránh chạy đồng thời nếu một lần quét mất hơn SCAN_INTERVAL
  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  private ScheduledFuture<?> scheduledTask;
  private final ConcurrentHashMap<Integer, ReentrantLock> sessionLocks;

  public AuctionClosingService(
      AuctionSessionDAO sessionDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      BidTransactionDAO bidDAO,
      AutoBidDAO autoBidDAO,
      BalanceService balanceService,
      AuctionEventBus eventBus,
      ConcurrentHashMap<Integer, ReentrantLock> sessionLocks) {
    this.sessionDAO  = sessionDAO;
    this.itemDAO     = itemDAO;
    this.userDAO     = userDAO;
    this.bidDAO      = bidDAO;
    this.autoBidDAO  = autoBidDAO;
    this.balanceService = balanceService;
    this.eventBus = eventBus;
    this.sessionLocks = sessionLocks;
  }

  // ════════════════════════════════════════════════════
  // LIFECYCLE — khởi động và dừng scheduler
  // ════════════════════════════════════════════════════

  /**
   * Khởi động scheduler.
   * Gọi một lần duy nhất khi server khởi động.
   */
  public void start() {
    scheduledTask = scheduler.scheduleAtFixedRate(
        this::scanAndCloseExpired,
        INITIAL_DELAY_SECONDS,
        SCAN_INTERVAL_SECONDS,
        TimeUnit.SECONDS);

    logger.info(
        "AuctionClosingService started — scan every {}s, initial delay {}s",
        SCAN_INTERVAL_SECONDS, INITIAL_DELAY_SECONDS);
  }

  /**
   * Dừng scheduler.
   * Gọi khi server shutdown để tránh thread leak.
   */
  public void stop() {
    if (scheduledTask != null) {
      scheduledTask.cancel(false);
    }
    scheduler.shutdown();
    logger.info("AuctionClosingService stopped");
  }

  // ════════════════════════════════════════════════════
  // CORE — quét và đóng phiên hết giờ
  // ════════════════════════════════════════════════════

  /**
   * Entry point của scheduler — được gọi mỗi 30 giây.
   *
   * <p>Dùng {@link AtomicBoolean} để tránh hai lần quét
   * chạy đồng thời nếu vì lý do nào đó lần trước chưa xong.
   */
  private void scanAndCloseExpired() {
    // Tránh chạy đồng thời
    if (!isRunning.compareAndSet(false, true)) {
      logger.warn("Bỏ qua lần quét này — lần trước vẫn đang chạy");
      return;
    }

    try {
      List<AuctionSession> expiredSessions =
          sessionDAO.findExpiredActive();
          
      List<AuctionSession> upcomingSessions =
          sessionDAO.findUpcomingToStart();

      if (expiredSessions.isEmpty() && upcomingSessions.isEmpty()) {
        logger.debug("Không có phiên hết giờ cần đóng hoặc phiên mới cần mở");
        return;
      }

      logger.info("Tìm thấy {} phiên hết giờ cần đóng, {} phiên cần mở", expiredSessions.size(), upcomingSessions.size());

      for (AuctionSession session : expiredSessions) {
        // Xử lý độc lập — lỗi một phiên không dừng các phiên khác
        closeSessionSafely(session);
      }
      
      for (AuctionSession session : upcomingSessions) {
        openSessionSafely(session);
      }

    } catch (SQLException e) {
      logger.error("Lỗi khi query phiên hết giờ: {}", e.getMessage());
    } finally {
      isRunning.set(false);
    }
  }

  /**
   * Bọc {@link #closeSession} trong try-catch để lỗi
   * một phiên không ảnh hưởng các phiên còn lại.
   */
  private void closeSessionSafely(AuctionSession session) {
    ReentrantLock lock = sessionLocks.computeIfAbsent(session.getId(), id -> new ReentrantLock());
    lock.lock();
    try {
      closeSession(session);
    } catch (Exception e) {
      logger.error(
          "Không thể đóng session sessionId={}, itemId={}: {}",
          session.getId(), session.getItemId(), e.getMessage());
    } finally {
      lock.unlock();
    }
  }

  private void openSessionSafely(AuctionSession session) {
    ReentrantLock lock = sessionLocks.computeIfAbsent(session.getId(), id -> new ReentrantLock());
    lock.lock();
    try {
      openSession(session);
    } catch (Exception e) {
      logger.error(
          "Không thể mở session sessionId={}, itemId={}: {}",
          session.getId(), session.getItemId(), e.getMessage());
    } finally {
      lock.unlock();
    }
  }
  
  private void openSession(AuctionSession session) throws SQLException {
      int sessionId = session.getId();
      int itemId = session.getItemId();
      logger.info("Mở session sessionId={}, itemId={}", sessionId, itemId);
      
      boolean transitioned = session.transitionStatus(SessionStatus.UPCOMING, SessionStatus.ACTIVE);
      if (!transitioned) {
          logger.warn("Không thể chuyển session {} sang ACTIVE — trạng thái hiện tại: {}", sessionId, session.getStatus());
          return;
      }
      sessionDAO.updateStatus(sessionId, SessionStatus.ACTIVE);
      itemDAO.updateStatus(itemId, ItemStatus.LISTED);
      
      vn.edu.vnu.uet.group8.server.service.auction.event.AuctionOpenedEvent event = 
          new vn.edu.vnu.uet.group8.server.service.auction.event.AuctionOpenedEvent(
              itemId, sessionId, session.getStartingPrice(), session.getEndTime()
          );
      eventBus.publish(event);
  }

  private void closeSession(AuctionSession session) throws SQLException {
    int sessionId = session.getId();
    int itemId    = session.getItemId();

    logger.info("Đóng session sessionId={}, itemId={}", sessionId, itemId);

    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      conn.setAutoCommit(false);
      try {
        // 1. Lock DB Row
        AuctionSession lockedSession = sessionDAO.lockSessionForUpdate(conn, sessionId)
            .orElseThrow(() -> new SQLException("Phiên không tồn tại hoặc đã xóa: " + sessionId));

        if (lockedSession.getStatus() != SessionStatus.ACTIVE) {
            logger.warn("Session {} không ở trạng thái ACTIVE, bỏ qua", sessionId);
            return;
        }

        Integer winnerId = lockedSession.getHighestBidderId();
        String itemTitle = itemDAO.findById(itemId).map(Item::getTitle).orElse("Sản phẩm #" + itemId);

        if (winnerId != null) {
            // SOLD
            BigDecimal finalPrice = lockedSession.getCurrentPrice();
            
            // Tìm Held Amount (Bảo chứng)
            BigDecimal heldAmount = finalPrice; // Mặc định ít nhất là giá chốt (nếu họ đang giữ đỉnh bằng lệnh thủ công)
            List<AutoBidConfig> configs = autoBidDAO.findActiveBySession(conn, sessionId);
            for (AutoBidConfig c : configs) {
                if (c.getUserId() == winnerId && c.getMaxPrice().compareTo(finalPrice) > 0) {
                    heldAmount = c.getMaxPrice();
                    break;
                }
            }

            // Cập nhật trạng thái
            boolean transitioned = lockedSession.transitionStatus(SessionStatus.ACTIVE, SessionStatus.SOLD);
            if (!transitioned) {
                throw new SQLException("Không thể chuyển sang SOLD");
            }
            sessionDAO.updateStatus(conn, sessionId, SessionStatus.SOLD);
            itemDAO.updateStatus(conn, itemId, ItemStatus.SOLD);

            // Xử lý ví tiền
            int sellerId = itemDAO.findById(itemId).map(Item::getSellerId)
                .orElseThrow(() -> new SQLException("Không tìm thấy item"));
            
            String winnerTxId = UUID.randomUUID().toString();
            String refundTxId = UUID.randomUUID().toString();
            String sellerTxId = UUID.randomUUID().toString();

            userDAO.settleAuctionPayment(conn, winnerTxId, refundTxId, sellerTxId, winnerId, sellerId, sessionId, heldAmount, finalPrice);

            conn.commit();

            // Broadcast Event
            String winnerUsername = userDAO.findById(winnerId).map(vn.edu.vnu.uet.group8.common.entity.User::getUsername).orElse("user#" + winnerId);
            logger.info("Session SOLD: sessionId={}, itemId={}, winner={}, price={}, heldAmount={}", sessionId, itemId, winnerUsername, finalPrice, heldAmount);
            try {
                eventBus.publish(AuctionEndedEvent.sold(itemId, itemTitle, finalPrice, winnerId, winnerUsername, sellerId));
            } catch (Exception e) {
                logger.error("Lỗi khi publish AuctionEndedEvent (SOLD): {}", e.getMessage());
            }

        } else {
            // ENDED_NO_BID
            boolean transitioned = lockedSession.transitionStatus(SessionStatus.ACTIVE, SessionStatus.ENDED_NO_BID);
            if (!transitioned) {
                throw new SQLException("Không thể chuyển sang ENDED_NO_BID");
            }
            sessionDAO.updateStatus(conn, sessionId, SessionStatus.ENDED_NO_BID);
            itemDAO.updateStatus(conn, itemId, ItemStatus.UNSOLD);

            conn.commit();

            // Broadcast Event
            int sellerId = itemDAO.findById(itemId).map(Item::getSellerId).orElse(0);
            logger.info("Session ENDED_NO_BID: sessionId={}, itemId={}", sessionId, itemId);
            try {
                eventBus.publish(AuctionEndedEvent.noBid(itemId, itemTitle, sellerId));
            } catch (Exception e) {
                logger.error("Lỗi khi publish AuctionEndedEvent (NO_BID): {}", e.getMessage());
            }
        }
      } catch (Exception e) {
          conn.rollback();
          throw new SQLException("Lỗi trong transaction đóng phiên", e);
      }
    }
  }
}