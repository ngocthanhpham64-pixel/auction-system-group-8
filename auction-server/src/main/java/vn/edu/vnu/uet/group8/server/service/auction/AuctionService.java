package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.model.BidRecord;
import vn.edu.vnu.uet.group8.common.dto.model.UserBidHistoryDTO;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;
import vn.edu.vnu.uet.group8.common.exception.AuctionException;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.BidHistoryEntry;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.UserBidRecord;
import vn.edu.vnu.uet.group8.server.service.auction.AntiSnipingService.AntiSnipingResult;
import vn.edu.vnu.uet.group8.server.service.auction.event.AuctionCancelledEvent;
import vn.edu.vnu.uet.group8.server.service.auction.event.AuctionOpenedEvent;
import vn.edu.vnu.uet.group8.server.service.auction.event.BidPlacedEvent;

/**
 * Orchestrator điều phối toàn bộ luồng đặt giá.
 *
 * <p><b>Nguyên tắc thiết kế:</b>
 * <ul>
 *   <li>Không chứa logic nghiệp vụ — ủy quyền cho các class chuyên biệt
 *   <li>Không build {@code ServerResponse} — publish event cho subscriber lo
 *   <li>Không biết gì về Socket, ClientHandler hay GSON
 *   <li>Quản lý lock theo từng phiên (fine-grained) — lấy từ code cũ
 * </ul>
 *
 * <p><b>Luồng placeBid():</b>
 * <pre>
 *   Lock phiên
 *     → BidValidator.validate()        kiểm tra mọi điều kiện
 *     → BidProcessor.process()         atomic DB: update giá + xử lý tiền
 *   Unlock phiên
 *     → AntiSnipingService.check()     gia hạn nếu cần (ngoài lock)
 *     → eventBus.publish()             notify subscriber (ngoài lock)
 * </pre>
 *
 * <p><b>Tại sao AntiSniping và EventBus nằm ngoài lock:</b>
 * Lock chỉ bảo vệ đoạn tính toán giá và cập nhật DB nguyên tử.
 * Giữ lock trong khi gọi mạng hoặc DB khác là nguyên nhân
 * phổ biến nhất của deadlock và nghẽn cổ chai.
 */
public class AuctionService {

  private static final Logger logger =
      LoggerFactory.getLogger(AuctionService.class);

  // ── Fine-grained locking — lấy từ code cũ ───────────
  // Mỗi phiên có một lock riêng — các phiên khác nhau
  // không block lẫn nhau. ConcurrentHashMap đảm bảo
  // thread-safe khi thêm/xóa lock.
  private final ConcurrentHashMap<Integer, ReentrantLock> sessionLocks =
      new ConcurrentHashMap<>();

  public ConcurrentHashMap<Integer, ReentrantLock> getSessionLocks() {
      return sessionLocks;
  }

  private final AuctionSessionDAO sessionDAO;
  private final BidValidator      validator;
  private final HybridBidExecutor executor;
  private final AntiSnipingService antiSniping;
  private final AuctionEventBus   eventBus;
  private final BidTransactionDAO bidTransactionDAO;
  private final AutoBidService autoBidService;

  public AuctionService(
      AuctionSessionDAO sessionDAO,
      BidValidator validator,
      HybridBidExecutor executor,
      AntiSnipingService antiSniping,
      AuctionEventBus eventBus,
      BidTransactionDAO bidTransactionDAO,
      AutoBidService autoBidService) {
    this.sessionDAO  = sessionDAO;
    this.validator   = validator;
    this.executor    = executor;
    this.antiSniping = antiSniping;
    this.eventBus    = eventBus;
    this.bidTransactionDAO = bidTransactionDAO;
    this.autoBidService = autoBidService;
  }

  // ════════════════════════════════════════════════════
  // ĐẶT GIÁ — Luồng chính của hệ thống đấu giá
  // ════════════════════════════════════════════════════

  /**
   * Xử lý một lần đặt giá từ Client.
   *
   * <p>Lock chỉ bao quanh validate + process — hai thao tác
   * phải là atomic với nhau. AntiSniping và publish event
   * nằm ngoài lock để không block các bid khác.
   *
   * @param bidderId  ID người đặt giá
   * @param sessionId ID phiên đấu giá
   * @param bidAmount số tiền muốn đặt
   * @throws AuctionException  nếu vi phạm nghiệp vụ đấu giá
   * @throws ItemNotFoundException nếu phiên không tồn tại
   * @throws SQLException      nếu lỗi DB
   */
  public BidResult placeBid(int bidderId, int itemId, BigDecimal bidAmount)
      throws SQLException {

    logger.info(
        "Nhận bid: bidderId={}, itemId={}, amount={}",
        bidderId, itemId, bidAmount);

    // Bước 1: Tìm sessionId đang hoạt động cho itemId.
    // Service chịu trách nhiệm cho việc mapping này, giúp Controller đơn giản hơn.
    AuctionSession session = sessionDAO.findActiveSessionByItemId(itemId)
        .orElseThrow(() -> new AuctionException(
            "Không có phiên đấu giá nào đang hoạt động cho sản phẩm #" + itemId));
    int sessionId = session.getId();

    BidResult bidResult;
    AntiSnipingResult snipingResult;

    // Bước 2: Validate các điều kiện cơ bản (sẽ validate kĩ hơn trong BidProcessor với row lock)
    BidContext context = validator.validate(
        bidderId, sessionId, bidAmount, false);

    // Bước 3: Xử lý DB atomic với thuật toán O(1) in-memory và row lock (Thủ công -> isAuto = false)
    bidResult = executor.execute(context, false);

    // Bước 5: Anti-sniping — gia hạn nếu bid trong cửa sổ cuối
    snipingResult = antiSniping.checkAndExtend(context.getAuctionSession());

    // Bước 5 (ngoài lock): Publish event — subscriber tự lo broadcast
    // Service không biết ai lắng nghe hay gửi như thế nào
    publishBidPlacedEvent(bidResult, snipingResult);

    logger.info(
        "Bid thành công: itemId={}, sessionId={}, bidderId={}, newPrice={}, isExtended={}",
        itemId,
        sessionId,
        bidderId,
        bidResult.getNewPrice(),
        snipingResult.isExtended());

    return bidResult;
  }

  // ════════════════════════════════════════════════════
  // QUẢN LÝ VÒNG ĐỜI PHIÊN ĐẤU GIÁ
  // ════════════════════════════════════════════════════

  /**
   * Mở phiên đấu giá — chuyển UPCOMING → ACTIVE.
   *
   * <p>Thường được gọi bởi {@link AuctionClosingService}
   * hoặc scheduler khi đến {@code startTime}.
   *
   * @throws AuctionException nếu phiên không ở trạng thái UPCOMING
   * @throws ItemNotFoundException nếu phiên không tồn tại
   * @throws SQLException nếu lỗi DB
   */
  public void openSession(int sessionId) throws SQLException {
    ReentrantLock lock = sessionLocks
        .computeIfAbsent(sessionId, id -> new ReentrantLock());

    lock.lock();
    try {
      AuctionSession session = loadSessionOrThrow(sessionId);

      boolean ok = session.transitionStatus(
          SessionStatus.UPCOMING, SessionStatus.ACTIVE);

      if (!ok) {
        throw new AuctionException(
            "Không thể mở phiên — trạng thái hiện tại: "
                + session.getStatus());
      }

      sessionDAO.updateStatus(sessionId, SessionStatus.ACTIVE);

      logger.info("Mở phiên đấu giá: sessionId={}, itemId={}",
          sessionId, session.getItemId());

      // Publish event để Client biết phiên bắt đầu
      eventBus.publish(new AuctionOpenedEvent(
          session.getItemId(),
          sessionId,
          session.getStartingPrice(),
          session.getEndTime()));

    } finally {
      lock.unlock();
    }
  }

  /**
   * Hủy phiên đấu giá — chuyển UPCOMING hoặc ACTIVE → CANCELLED.
   *
   * <p>Chỉ Admin gọi. Nếu đang ACTIVE phải hoàn tiền cho bidder hiện tại.
   *
   * @param cancelledByAdminId ID admin thực hiện hủy
   * @throws AuctionException  nếu không thể hủy ở trạng thái hiện tại
   * @throws ItemNotFoundException nếu phiên không tồn tại
   * @throws SQLException      nếu lỗi DB
   */
  public void cancelSession(int sessionId, int cancelledByAdminId)
      throws SQLException {
    ReentrantLock lock = sessionLocks
        .computeIfAbsent(sessionId, id -> new ReentrantLock());

    lock.lock();
    try {
      AuctionSession session = loadSessionOrThrow(sessionId);

      // Thử chuyển từ UPCOMING trước, nếu không thì từ ACTIVE
      boolean ok = session.transitionStatus(
          SessionStatus.UPCOMING, SessionStatus.CANCELLED);

      if (!ok) {
        ok = session.transitionStatus(
            SessionStatus.ACTIVE, SessionStatus.CANCELLED);
      }

      if (!ok) {
        throw new AuctionException(
            "Không thể hủy phiên — trạng thái hiện tại: "
                + session.getStatus()
                + ". Chỉ hủy được UPCOMING hoặc ACTIVE");
      }

        // Hoàn tiền cho bidder đang dẫn đầu nếu phiên đang ACTIVE và đã có bid
        if (session.getHighestBidderId() != null && session.getCurrentPrice() != null) {
            bidTransactionDAO.refundBidderExternal(session.getHighestBidderId(), session.getCurrentPrice(), sessionId);
            logger.info("Đã hoàn tiền {} cho user {} do phiên đấu giá {} bị hủy", 
                session.getCurrentPrice(), session.getHighestBidderId(), sessionId);
        }

      sessionDAO.updateStatus(sessionId, SessionStatus.CANCELLED);

      logger.info(
          "Hủy phiên: sessionId={}, itemId={}, adminId={}",
          sessionId, session.getItemId(), cancelledByAdminId);

      // Publish event — subscriber lo hoàn tiền và broadcast
      eventBus.publish(new AuctionCancelledEvent(
          session.getItemId(),
          sessionId,
          session.getHighestBidderId(),
          session.getCurrentPrice()));

    } finally {
      lock.unlock();
    }
  }

  /**
   * Dọn dẹp lock của phiên đã kết thúc — tránh memory leak.
   *
   * <p>Gọi bởi {@link AuctionClosingService} sau khi đóng phiên
   * thành công. Không gọi khi phiên đang có bid đang chờ.
   *
   * @param sessionId ID phiên đã kết thúc (SOLD hoặc ENDED_NO_BID)
   */
  public void releaseSessionLock(int sessionId) {
    ReentrantLock removed = sessionLocks.remove(sessionId);
    if (removed != null) {
      logger.debug("Đã release lock cho sessionId={}", sessionId);
    }
  }

  // ════════════════════════════════════════════════════
  // Truy vấn đấu giá
  // ════════════════════════════════════════════════════
  /**
   * Lấy lịch sử đặt giá của một phiên đấu giá.
   *
   */
  public List<BidRecord> getItemBidHistory(int itemId) throws SQLException {
    List<AuctionSession> sessions = sessionDAO.findByItemId(itemId);

    if (sessions.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    
    AuctionSession session = sessions.get(0);
        
    List<BidHistoryEntry> history = bidTransactionDAO.findHistoryByItem(session.getId());
    
    return history.stream().map(h -> BidRecord.builder()
        .bidId(h.bidId())
        .itemId(itemId)
        .userId(h.bidderId())
        .displayName(h.bidderUsername())
        .amount(h.bidAmount())
        .status(h.status())
        .placedAt(h.bidTime())
        .build())
        .collect(Collectors.toList());
  }

  /**
   * Lấy lịch sử đặt giá của một người dùng.
   *
   * <p>Kết hợp thông tin từ bảng bid_transaction, auction_session và item
   * để cung cấp cái nhìn tổng quan về các phiên người dùng đã tham gia.
   
   * @param userId
   * @return
   * @throws SQLException
   */
  public List<UserBidHistoryDTO> getUserBidHistory(int userId) throws SQLException {
    List<UserBidRecord> records = bidTransactionDAO.findHistoryByUser(userId);
    
    return records.stream()
        .map(r -> UserBidHistoryDTO.of(
          r.bidId(),
          r.sessionId(),
          r.itemId(),
          r.itemTitle(),
          r.bidAmount(),
          r.currentPrice(),
          r.bidTime(),
          r.sessionEndTime()
        ))
        .collect(Collectors.toList());
  }
  // ════════════════════════════════════════════════════
  // PRIVATE HELPERS
  // ════════════════════════════════════════════════════

  /**
   * Load AuctionSession hoặc ném exception nếu không tìm thấy.
   * Tập trung xử lý Optional.empty() ở một chỗ duy nhất.
   */
  private AuctionSession loadSessionOrThrow(int sessionId)
      throws SQLException {
    return sessionDAO
        .findById(sessionId)
        .orElseThrow(() -> new ItemNotFoundException(
            "Không tìm thấy phiên đấu giá với id=" + sessionId));
  }

  public boolean hasActiveAutoBid(int userId, int itemId) throws SQLException {
      AuctionSession session = sessionDAO.findActiveSessionByItemId(itemId).orElse(null);
      if (session == null) return false;
      return autoBidService.hasActiveAutoBid(userId, session.getId());
  }

  public void cancelAutoBid(int userId, int itemId) throws SQLException {
      AuctionSession session = sessionDAO.findActiveSessionByItemId(itemId).orElse(null);
      if (session == null) return;
      ReentrantLock lock = sessionLocks.computeIfAbsent(session.getId(), id -> new ReentrantLock());
      lock.lock();
      try {
          autoBidService.cancelAutoBid(userId, session.getId());
      } finally {
          lock.unlock();
      }
  }

  /**
   * Kích hoạt cấu hình Proxy Auto-Bid.
   * Gộp chung logic với placeBid vì bản chất thao tác Manual Bid
   * cũng được coi là một Autobid dùng 1 lần trong thuật toán O(1).
   */
  public BidResult placeAutoBid(int userId, int itemId, BigDecimal maxPrice) throws SQLException {
    logger.info("Nhận auto-bid config: userId={}, itemId={}, maxPrice={}", userId, itemId, maxPrice);

    AuctionSession session = sessionDAO.findActiveSessionByItemId(itemId)
        .orElseThrow(() -> new AuctionException("Không có phiên đấu giá nào đang hoạt động cho sản phẩm #" + itemId));
    int sessionId = session.getId();

    BidResult bidResult;
    AntiSnipingResult snipingResult;

    // Cờ isSystemDefense = false vì đây là User chủ động đặt cấu hình
    BidContext context = validator.validate(
        userId, sessionId, maxPrice, false);

    // Xử lý DB atomic với thuật toán O(1) in-memory (Tự động -> isAuto = true)
    bidResult = executor.execute(context, true);

    // Anti-sniping
    snipingResult = antiSniping.checkAndExtend(context.getAuctionSession());

    // Publish event
    publishBidPlacedEvent(bidResult, snipingResult);

    return bidResult;
  }

  /**
   * Build và publish {@link BidPlacedEvent}.
   *
   * <p>Tách thành method riêng để {@code placeBid()} gọn hơn
   * và dễ test: chỉ cần verify eventBus.publish() được gọi
   * với đúng tham số.
   */
  private void publishBidPlacedEvent(
      BidResult bidResult,
      AntiSnipingResult snipingResult) {

    eventBus.publish(new BidPlacedEvent(
        bidResult.getItemId(),
        bidResult.getBidderId(),
        bidResult.getBidderUsername(),
        bidResult.getNewPrice(),
        bidResult.getTotalBids(),
        snipingResult.getNewEndTime(),
        snipingResult.isExtended(),
        bidResult.hasPreviousBidder()
            ? bidResult.getPrevBidderId()
            : null));
  }
}