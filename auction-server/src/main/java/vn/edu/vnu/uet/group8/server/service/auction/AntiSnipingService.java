package vn.edu.vnu.uet.group8.server.service.auction;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;

/**
 * Xử lý nghiệp vụ Anti-sniping — gia hạn thời gian đấu giá
 * khi có bid xuất hiện trong cửa sổ cuối phiên.
 *
 * <p><b>Vấn đề Anti-sniping là gì:</b> Người dùng cố tình chờ
 * đến giây cuối cùng mới đặt giá để không ai kịp phản ứng.
 * Hệ thống giải quyết bằng cách tự động gia hạn thêm thời gian
 * mỗi khi có bid trong {@code SNIPE_WINDOW_MINUTES} phút cuối.
 *
 * <p><b>Ví dụ:</b>
 * <pre>
 *   endTime = 20:00:00
 *   Bid xuất hiện lúc 19:56:30 (trong 5 phút cuối)
 *   → gia hạn thêm 5 phút
 *   → endTime mới = 20:01:30
 *
 *   Bid tiếp theo lúc 20:03:00 (vẫn trong 5 phút cuối của endTime mới)
 *   → gia hạn thêm 5 phút nữa
 *   → endTime mới = 20:06:00
 * </pre>
 *
 * <p>Không có giới hạn số lần gia hạn — phiên chỉ kết thúc khi
 * không còn bid nào trong cửa sổ cuối.
 *
 * <p><b>Trách nhiệm duy nhất:</b> Kiểm tra và gia hạn.
 * Không broadcast, không validate bid, không xử lý tiền.
 */
public class AntiSnipingService {

  private static final Logger logger =
      LoggerFactory.getLogger(AntiSnipingService.class);

  /**
   * Số phút cuối phiên để kích hoạt anti-sniping.
   * Bid trong khoảng này → gia hạn thêm.
   */
  private static final int SNIPE_WINDOW_MINUTES = 5;

  /**
   * Số phút gia hạn thêm mỗi lần bị snipe.
   */
  private static final int SNIPE_EXTENSION_MINUTES = 5;

  private final AuctionSessionDAO sessionDAO;

  public AntiSnipingService(AuctionSessionDAO sessionDAO) {
    this.sessionDAO = sessionDAO;
  }

  // ════════════════════════════════════════════════════
  // PUBLIC API
  // ════════════════════════════════════════════════════

  /**
   * Kiểm tra và gia hạn phiên nếu bid nằm trong khoảng thời gian anti-sniping.
   *
   * <p>Được gọi bởi {@link AuctionService} SAU KHI {@link BidProcessor}
   * đã xử lý bid thành công và SAU KHI đã unlock {@code ReentrantLock}.
   * Thứ tự này đảm bảo:
   * <ul>
   *   <li>Bid đã được lưu DB trước khi gia hạn
   *   <li>Không giữ lock trong khi gọi DB — tránh nghẽn cổ chai
   * </ul>
   *
   * @param session phiên đấu giá vừa có bid mới — đã được load
   *                bởi {@link BidValidator}, không cần load lại
   * @return {@link AntiSnipingResult} chứa endTime mới và trạng thái
   *         có gia hạn hay không — {@link AuctionService} dùng để
   *         build BidResult và publish event
   * @throws SQLException nếu lỗi khi cập nhật endTime vào DB
   */
  public AntiSnipingResult checkAndExtend(AuctionSession session)
      throws SQLException {

    Instant currentEndTime = session.getEndTime();

    // Kiểm tra bid có nằm trong cửa sổ anti-sniping không
    // isInSnipingWindow() dùng Instant.now() so với endTime
    if (!session.isInSnipingWindow(SNIPE_WINDOW_MINUTES)) {
      logger.debug(
          "sessionId={} không trong snipe window — bỏ qua",
          session.getId());
      return AntiSnipingResult.notExtended(currentEndTime);
    }

    // Tính endTime mới
    Instant newEndTime = currentEndTime
        .plus(SNIPE_EXTENSION_MINUTES, ChronoUnit.MINUTES);

    // Cập nhật DB
    sessionDAO.updateEndTime(session.getId(), newEndTime);

    logger.info(
        "Anti-sniping kích hoạt: sessionId={}, itemId={}, "
            + "original time={}, endTime cũ={}, endTime mới={}",
        session.getId(), session.getItemId(),
        session.getStartTime(), currentEndTime, newEndTime);

    return AntiSnipingResult.extended(newEndTime, currentEndTime);
  }

  // ════════════════════════════════════════════════════
  // RESULT OBJECT — immutable, không có side effect
  // ════════════════════════════════════════════════════

  /**
   * Kết quả sau khi kiểm tra anti-sniping.
   *
   * <p>Immutable — {@link AuctionService} dùng để:
   * <ul>
   *   <li>Build {@link BidResult} với endTime chính xác
   *   <li>Quyết định có đưa {@code isExtended = true}
   *       vào broadcast event không
   * </ul>
   */
  public static final class AntiSnipingResult {

    private final boolean extended;
    private final Instant newEndTime;

    /**
     * endTime trước khi gia hạn — null nếu không gia hạn.
     * Dùng để log so sánh trước/sau.
     */
    private final Instant previousEndTime;

    private AntiSnipingResult(
        boolean extended, Instant newEndTime, Instant previousEndTime) {
      this.extended        = extended;
      this.newEndTime      = newEndTime;
      this.previousEndTime = previousEndTime;
    }

    /** Tạo result khi KHÔNG gia hạn. */
    static AntiSnipingResult notExtended(Instant currentEndTime) {
      return new AntiSnipingResult(false, currentEndTime, null);
    }

    /** Tạo result khi CÓ gia hạn. */
    static AntiSnipingResult extended(
        Instant newEndTime, Instant previousEndTime) {
      return new AntiSnipingResult(true, newEndTime, previousEndTime);
    }

    /**
     * @return {@code true} nếu phiên vừa được gia hạn
     */
    public boolean isExtended() {
      return extended;
    }

    /**
     * @return endTime hiện tại — đã gia hạn nếu {@link #isExtended()}
     *         là {@code true}, giữ nguyên nếu {@code false}
     */
    public Instant getNewEndTime() {
      return newEndTime;
    }

    /**
     * Số giây đã gia hạn thêm — 0 nếu không gia hạn.
     * Dùng cho log và debug.
     */
    // public long getExtensionSeconds() {
    //   if (!extended || previousEndTime == null) return 0;
    //   return newEndTime.getEpochSecond()
    //       - session.getStartTime().getEpochSecond();
    // }

    @Override
    public String toString() {
      return "AntiSnipingResult{"
          + "extended=" + extended
          + ", newEndTime=" + newEndTime
          // + ", extensionSeconds=" + getExtensionSeconds()
          + '}';
    }
  }
}