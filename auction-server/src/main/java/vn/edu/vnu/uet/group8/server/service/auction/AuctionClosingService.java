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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.LeaderInfo;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

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
  private final AuctionBroadcaster broadcaster;

  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread t = new Thread(runnable, "auction-closing-scheduler");
        t.setDaemon(true);
        return t;
      });

  // Tránh chạy đồng thời nếu một lần quét mất hơn SCAN_INTERVAL
  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  private ScheduledFuture<?> scheduledTask;

  public AuctionClosingService(
      AuctionSessionDAO sessionDAO,
      ItemDAO itemDAO,
      UserDAO userDAO,
      BidTransactionDAO bidDAO,
      AuctionBroadcaster broadcaster) {
    this.sessionDAO  = sessionDAO;
    this.itemDAO     = itemDAO;
    this.userDAO     = userDAO;
    this.bidDAO      = bidDAO;
    this.broadcaster = broadcaster;
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

      if (expiredSessions.isEmpty()) {
        logger.debug("Không có phiên hết giờ cần đóng");
        return;
      }

      logger.info("Tìm thấy {} phiên hết giờ cần đóng", expiredSessions.size());

      for (AuctionSession session : expiredSessions) {
        // Xử lý độc lập — lỗi một phiên không dừng các phiên khác
        closeSessionSafely(session);
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
    try {
      closeSession(session);
    } catch (Exception e) {
      logger.error(
          "Không thể đóng session sessionId={}, itemId={}: {}",
          session.getId(), session.getItemId(), e.getMessage());
    }
  }

  /**
   * Đóng một phiên đấu giá cụ thể.
   *
   * <p>Tìm người dẫn đầu → quyết định SOLD hay ENDED_NO_BID
   * → cập nhật DB → broadcast.
   */
  private void closeSession(AuctionSession session) throws SQLException {
    int sessionId = session.getId();
    int itemId    = session.getItemId();

    logger.info("Đóng session sessionId={}, itemId={}", sessionId, itemId);

    // Tìm người đang dẫn đầu
    Optional<LeaderInfo> leaderOpt =
        bidDAO.findCurrentLeader(itemId);

    // Load Item để lấy title cho broadcast
    Optional<Item> itemOpt = itemDAO.findById(itemId);
    String itemTitle = itemOpt
        .map(Item::getTitle)
        .orElse("Sản phẩm #" + itemId);

    if (leaderOpt.isPresent()) {
      closeSold(session, leaderOpt.get(), itemTitle);
    } else {
      closeNoBid(session, itemTitle);
    }
  }

  // ════════════════════════════════════════════════════
  // XỬ LÝ KHI CÓ NGƯỜI THẮNG
  // ════════════════════════════════════════════════════

  /**
   * Xử lý phiên kết thúc có người thắng.
   *
   * <p>Thứ tự:
   * <ol>
   *   <li>Chuyển session → SOLD
   *   <li>Chuyển tiền (currentPrice) từ người mua sang seller
   *   <li>Broadcast AUCTION_ENDED (SOLD)
   * </ol>
   *
   * <p>Tiền đã được trừ từ winner lúc đặt giá — giờ
   * chỉ cần cộng vào ví seller.
   */
  private void closeSold(
      AuctionSession session,
      LeaderInfo leader,
      String itemTitle) throws SQLException {

    int sessionId  = session.getId();
    int itemId     = session.getItemId();
    int winnerId   = leader.bidderId();
    BigDecimal finalPrice = session.getCurrentPrice();

    // Bước 1: Chuyển trạng thái session → SOLD
    boolean transitioned =
        session.transitionStatus(ItemStatus.ACTIVE, ItemStatus.SOLD);

    if (!transitioned) {
      logger.warn(
          "Không thể chuyển session {} sang SOLD — trạng thái hiện tại: {}",
          sessionId, session.getStatus());
      return;
    }

    sessionDAO.updateStatus(sessionId, ItemStatus.SOLD);

    // Bước 2: Chuyển tiền cho seller
    // Tiền của winner đã bị trừ lúc placeBid()
    // Giờ cộng vào ví seller — DAO lo atomic
    int sellerId = itemDAO.findById(itemId)
        .map(Item::getSellerId)
        .orElseThrow(() -> new SQLException(
            "Không tìm thấy item itemId=" + itemId));

    userDAO.updateBalance(sellerId, finalPrice);

    // Bước 3: Lấy username của winner để broadcast
    String winnerUsername = userDAO.findById(winnerId)
        .map(u -> u.getUsername())
        .orElse("user#" + winnerId);

    logger.info(
        "Session SOLD: sessionId={}, itemId={}, winner={}, price={}",
        sessionId, itemId, winnerUsername, finalPrice);

    // Bước 4: Broadcast — SAU KHI đã commit DB
    broadcaster.broadcastAuctionSold(
        itemId, itemTitle, finalPrice, winnerUsername);
  }

  // ════════════════════════════════════════════════════
  // XỬ LÝ KHI KHÔNG CÓ BID
  // ════════════════════════════════════════════════════

  /**
   * Xử lý phiên kết thúc không có bid nào.
   *
   * <p>Không cần xử lý tiền vì không ai đặt giá.
   * Chỉ cần chuyển trạng thái và broadcast.
   */
  private void closeNoBid(
      AuctionSession session,
      String itemTitle) throws SQLException {

    int sessionId = session.getId();
    int itemId    = session.getItemId();

    boolean transitioned =
        session.transitionStatus(
            ItemStatus.ACTIVE, ItemStatus.ENDED_NO_BID);

    if (!transitioned) {
      logger.warn(
          "Không thể chuyển session {} sang ENDED_NO_BID — trạng thái: {}",
          sessionId, session.getStatus());
      return;
    }

    sessionDAO.updateStatus(sessionId, ItemStatus.ENDED_NO_BID);

    logger.info(
        "Session ENDED_NO_BID: sessionId={}, itemId={}",
        sessionId, itemId);

    broadcaster.broadcastAuctionNoBid(itemId, itemTitle);
  }
}