package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.exception.AuctionException;
import vn.edu.vnu.uet.group8.common.exception.ItemNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;

/**
 * Kiểm tra tính hợp lệ của một lần đặt giá.
 *
 * <p>Chỉ validate — không có side effect, không thay đổi DB.
 * Mọi vi phạm đều ném exception ngay lập tức (fail-fast).
 *
 * <p>Thứ tự kiểm tra được thiết kế có chủ đích:
 * <ol>
 *   <li>Item tồn tại và hợp lệ — kiểm tra trước để tránh NPE
 *   <li>Bidder tồn tại và có quyền
 *   <li>Domain rules — không cho tự bid
 *   <li>State checks — giá, số dư
 * </ol>
 */
public class BidValidator {

  private static final Logger logger = LoggerFactory.getLogger(BidValidator.class);

  /**
   * Bước giá tối thiểu: 1% giá hiện tại.
   * Tối thiểu tuyệt đối: 1,000 VND.
   */
  private static final BigDecimal MIN_BID_INCREMENT_RATE = new BigDecimal("0.01");
  private static final BigDecimal MIN_BID_INCREMENT_ABS = new BigDecimal("1000");

  private final ItemDAO itemDAO;
  private final UserDAO userDAO;
  private final AuctionSessionDAO auctionSessionDAO;

  public BidValidator(ItemDAO itemDAO, UserDAO userDAO, AuctionSessionDAO auctionSessionDAO) {
    this.itemDAO = itemDAO;
    this.userDAO = userDAO;
    this.auctionSessionDAO = auctionSessionDAO;
  }

  /**
   * Validate toàn bộ điều kiện của một lần đặt giá.
   *
   * <p>Trả về {@link BidContext} chứa các entity đã load — tránh load lại
   * ở {@link BidProcessor}.
   *
   * @param bidderId  ID người đặt giá
   * @param itemId    ID item muốn đặt
   * @param bidAmount số tiền muốn đặt
   * @return {@link BidContext} nếu hợp lệ
   * @throws ItemNotFoundException nếu item không tồn tại
   * @throws UserNotFoundException nếu bidder không tồn tại
   * @throws ValidationException   nếu vi phạm domain rule hoặc state check
   * @throws AuctionException      nếu item không ở trạng thái hợp lệ
   * @throws SQLException          nếu lỗi DB
   */
  public BidContext validate(int bidderId, int itemId, BigDecimal bidAmount)
      throws SQLException {

    logger.debug("Bắt đầu validate bid: bidderId={}, itemId={}, amount={}",
        bidderId, itemId, bidAmount);

    // -- Bước 1: Validate -- kiểm tra trước để tránh load bidder
    // khi item không tồn tại
    AuctionSession as = loadAndValidateItem(itemId);

    // Check item
    Item item = itemDAO.findById(as.getItemId())
        .orElseThrow(() -> new ItemNotFoundException(as.getItemId()));

    // -- Bước 2: Validate bidder
    User bidder = loadAndValidateBidder(bidderId);

    // -- Bước 3: Domain rules -- không phụ thuộc DB
    validateDomainRules(bidder, item, bidAmount);

    // -- Bước 4: State checks -- phụ thuộc giá trị hiện tại
    validateStateChecks(bidder, as, bidAmount);

    logger.debug("Validate bid thành công: bidderId={}, itemId={}", bidderId, itemId);

    // Trả context để BidProcessor dùng lại -- không load lại từ DB
    return new BidContext(bidder, item, as, bidAmount);
  }

  // -- Private: load và validate item ----------------------------------------

  private AuctionSession loadAndValidateItem(int itemId) throws SQLException {
    AuctionSession as = auctionSessionDAO
        .findById(itemId)
        .orElseThrow(() -> new ItemNotFoundException(itemId));
    
    if (as.getStatus() != ItemStatus.ACTIVE) {
      throw new AuctionException(
          "Sản phẩm không trong trạng thái đấu giá. "
              + "Trạng thái hiện tại: " + as.getStatus());
    }

    // Kiểm tra thời gian -- isExpired() dùng Instant.now() so với endTime
    if (as.isExpired()) {
      throw new AuctionException("Phiên đấu giá đã kết thúc");
    }

    return as;
  }

  // -- Private: load và validate bidder --------------------------------------

  private User loadAndValidateBidder(int bidderId) throws SQLException {
    User bidder = userDAO
        .findById(bidderId)
        .orElseThrow(() -> new UserNotFoundException(bidderId));

    if (!(bidder instanceof UserMember member)) {
      throw new ValidationException("Không thể đấu giá");
    }

    // canBid() = isActive()
    if (!member.isActive()) {
      throw new ValidationException(
          "Tài khoản không có quyền đặt giá. "
              + "Trạng thái: " + bidder.getStatus());
    }

    return bidder;
  }

  // -- Private: domain rules -- không phụ thuộc trạng thái DB ---------------

  private void validateDomainRules(User bidder, Item item, BigDecimal bidAmount) {

    // Seller không được tự đặt giá item của mình
    // Lấy từ code cũ -- quan trọng để tránh gian lận
    if (item.getSellerId() == bidder.getId()) {
      throw new ValidationException(
          "Không thể đặt giá cho sản phẩm của chính mình");
    }

    // bidAmount không được null hoặc âm
    if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ValidationException("Số tiền đặt giá phải lớn hơn 0");
    }
  }

  // -- Private: state checks -- phụ thuộc giá trị hiện tại của DB -----------

  private void validateStateChecks(User bidder, AuctionSession as, BigDecimal bidAmount) {

    if (!(bidder instanceof UserMember member)) {
      throw new ValidationException("Không thể đấu giá");
    }

    // Kiểm tra bước giá tối thiểu
    BigDecimal minNextBid = calculateMinNextBid(as.getCurrentPrice());
    if (bidAmount.compareTo(minNextBid) < 0) {
      throw new ValidationException(
          String.format(
              "Giá đặt tối thiểu là %s VND (hiện tại: %s VND, bước tối thiểu: %s VND)",
              minNextBid, as.getCurrentPrice(),
              minNextBid.subtract(as.getCurrentPrice())));
    }

    // Kiểm tra số dư ví >= bidAmount
    // Dùng compareTo vì BigDecimal -- không dùng < hay >
    if (member.getBalance().compareTo(bidAmount) < 0) {
      throw new ValidationException(
          String.format(
              "Số dư không đủ. Hiện có: %s VND, cần: %s VND",
              member.getBalance(), bidAmount));
    }
  }

  // -- Helper: tính giá tối thiểu cho lần bid tiếp theo ---------------------

  /**
   * Tính giá tối thiểu cho lần bid tiếp theo.
   *
   * <p>Công thức: {@code currentPrice + max(currentPrice * 1%, 1000)}
   *
   * <p>Ví dụ:
   * <ul>
   *   <li>currentPrice = 1,000,000 → minNext = 1,010,000 (1%)
   *   <li>currentPrice = 50,000    → minNext = 51,000    (1000 VND tuyệt đối)
   * </ul>
   */
  static BigDecimal calculateMinNextBid(BigDecimal currentPrice) {
    BigDecimal percentIncrement = currentPrice.multiply(MIN_BID_INCREMENT_RATE);
    BigDecimal increment = percentIncrement.compareTo(MIN_BID_INCREMENT_ABS) > 0
        ? percentIncrement
        : MIN_BID_INCREMENT_ABS;
    return currentPrice.add(increment);
  }
}