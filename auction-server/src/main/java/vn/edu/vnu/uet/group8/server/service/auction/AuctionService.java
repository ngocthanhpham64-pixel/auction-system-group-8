package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.exception.AuctionException;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
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

  private final AuctionSessionDAO sessionDAO;
  private final BidValidator      validator;
  private final BidProcessor      processor;
  private final AntiSnipingService antiSniping;
  private final AuctionEventBus   eventBus;

  public AuctionService(
      AuctionSessionDAO sessionDAO,
      BidValidator validator,
      BidProcessor processor,
      AntiSnipingService antiSniping,
      AuctionEventBus eventBus) {
    this.sessionDAO  = sessionDAO;
    this.validator   = validator;
    this.processor   = processor;
    this.antiSniping = antiSniping;
    this.eventBus    = eventBus;
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
  public void placeBid(int bidderId, int sessionId, BigDecimal bidAmount)
      throws SQLException {

    logger.info(
        "Nhận bid: bidderId={}, sessionId={}, amount={}",
        bidderId, sessionId, bidAmount);

    // ── Lấy lock của phiên này ────────────────────────
    // computeIfAbsent đảm bảo thread-safe khi tạo lock mới
    ReentrantLock lock = sessionLocks
        .computeIfAbsent(sessionId, id -> new ReentrantLock());

    BidResult bidResult;
    AntiSnipingResult snipingResult;

    // ════════════════════════════════════════════════
    // CRITICAL SECTION — chỉ một thread tại một thời điểm
    // ════════════════════════════════════════════════
    lock.lock();
    try {

      // Bước 1: Load session — trong lock để đảm bảo
      // trạng thái nhất quán với validate và process
      AuctionSession session = loadSessionOrThrow(sessionId);

      // Bước 2: Validate toàn bộ điều kiện
      // Trả BidContext chứa entity đã load — tránh load lại
      BidContext context = validator.validate(
          bidderId, session.getItemId(), bidAmount);

      // Bước 3: Xử lý DB atomic — update giá + xử lý tiền
      bidResult = processor.process(context);

    } finally {
      // LUÔN unlock — dù validate hay process ném exception
      lock.unlock();
    }
    // ════════════════════════════════════════════════
    // NGOÀI LOCK — không block các bid khác
    // ════════════════════════════════════════════════

    // Bước 4: Anti-sniping — gia hạn nếu bid trong cửa sổ cuối
    // Cần load lại session vì endTime có thể đã thay đổi
    AuctionSession sessionForSniping = loadSessionOrThrow(sessionId);
    snipingResult = antiSniping.checkAndExtend(sessionForSniping);

    // Bước 5: Publish event — subscriber tự lo broadcast
    // Service không biết ai lắng nghe hay gửi như thế nào
    publishBidPlacedEvent(bidResult, snipingResult);

    logger.info(
        "Bid thành công: sessionId={}, bidderId={}, "
            + "newPrice={}, isExtended={}",
        sessionId, bidderId,
        bidResult.getNewPrice(),
        snipingResult.isExtended());
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
          ItemStatus.UPCOMING, ItemStatus.ACTIVE);

      if (!ok) {
        throw new AuctionException(
            "Không thể mở phiên — trạng thái hiện tại: "
                + session.getStatus());
      }

      sessionDAO.updateStatus(sessionId, ItemStatus.ACTIVE);

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
          ItemStatus.UPCOMING, ItemStatus.CANCELLED);

      if (!ok) {
        ok = session.transitionStatus(
            ItemStatus.ACTIVE, ItemStatus.CANCELLED);
      }

      if (!ok) {
        throw new AuctionException(
            "Không thể hủy phiên — trạng thái hiện tại: "
                + session.getStatus()
                + ". Chỉ hủy được UPCOMING hoặc ACTIVE");
      }

      sessionDAO.updateStatus(sessionId, ItemStatus.CANCELLED);

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