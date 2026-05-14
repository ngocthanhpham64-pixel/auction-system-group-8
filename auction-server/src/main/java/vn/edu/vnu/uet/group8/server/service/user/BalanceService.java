package vn.edu.vnu.uet.group8.server.service.user;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.model.TransactionHistoryEntry;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.PaymentMethod;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;
import vn.edu.vnu.uet.group8.common.exception.DuplicateTransactionException;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.TransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**
 * Xử lý nghiệp vụ liên quan đến số dư ví.
 */
public class BalanceService {

  private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

  private final UserDAO userDAO;
  private final TransactionDAO transactionDAO;

  public BalanceService(UserDAO userDAO, TransactionDAO transactionDAO) {
    this.userDAO = userDAO;
    this.transactionDAO = transactionDAO;
  }

  /**
   * Nạp tiền vào ví với Idempotency Key.
   *
   * <p><b>Atomicity:</b> Việc lưu lịch sử giao dịch và cộng tiền
   * vào ví được thực hiện trong cùng một SQL transaction tại DAO.
   * Nếu một trong hai thất bại, cả hai sẽ bị rollback — không có
   * trường hợp tiền bị mất hoặc tiền được cộng mà không có lịch sử.
   *
   * <p><b>Idempotency:</b> {@code transactionId} duy nhất do Client
   * tạo (UUID) mỗi lần bấm nút. Server kiểm tra trong DB trước khi
   * thực thi — nếu đã tồn tại, ném exception thay vì cộng tiền lần 2.
   *
   * @param transactionId ID duy nhất do Client tạo, dùng UUID
   * @return BigDecimal balance mới sau khi nạp
   * @throws ValidationException           nếu số tiền hoặc ID không hợp lệ
   * @throws DuplicateTransactionException nếu transactionId đã xử lý
   * @throws UserNotFoundException         nếu user không tồn tại
   * @throws SQLException                  nếu lỗi DB
   */
  public BigDecimal topUpBalance(
      int userId, BigDecimal amount, String transactionId, PaymentMethod paymentMethod)
      throws SQLException {
    log.info("Đang nạp tiền cho id {} với số tiền {}", userId, amount);

    // -- Validate transactionId
    if (transactionId == null || transactionId.isBlank()) {
      throw new ValidationException("transactionId không được để trống");
    }

    // -- Validate số tiền -- UserPolicy tự ném nếu sai
    UserPolicy.validateTopUpAmount(amount);

    // -- Kiểm tra user tồn tại
    User user = userDAO.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // -- Kiểm tra balance tổng không vượt giới hạn
    if (!(user instanceof UserMember member)) {
      throw new ValidationException("Tài khoản này không hỗ trợ ví tiền.");
    }

    BigDecimal currentBalance = member.getBalance();
    if (currentBalance.add(amount).compareTo(UserPolicy.BALANCE_MAX_TOTAL) > 0) {
      throw new ValidationException(
          "Số dư ví không được vượt quá " + UserPolicy.BALANCE_MAX_TOTAL + " VND");
    }

    // -- Thực thi atomic: lưu lịch sử + cộng tiền trong 1 SQL transaction
    // Nếu bất kỳ bước nào thất bại → rollback cả hai
    // Không bao giờ xảy ra tình trạng "có lịch sử nhưng tiền chưa cộng"
    // hoặc "tiền đã cộng nhưng không có lịch sử"
    return userDAO.insertTransactionAndUpdateBalance(
        transactionId, userId, amount, TransactionType.DEPOSIT, paymentMethod);
  }

  /**
   * Rút tiền đồng thời ví với Idempotency Key.
   * 
   * @param transactionId                  ID duy nhất do Client tạo, dùng UUID
   * @return                               BigDecimal balance mới sau khi rút
   * @throws ValidationException           nếu số tiền hoặc ID không hợp lệ
   * @throws DuplicateTransactionException
   * @throws UserNotFoundException
   * @throws SQLException
   */
  public BigDecimal withdrawBalance(
      int userId, BigDecimal amount, String transactionId, PaymentMethod paymentMethod)
      throws SQLException {

    log.info("Đang rút tiền cho id {} với số tiền {}", userId, amount);
    
    // -- Validate transactionId
    if (transactionId == null || transactionId.isBlank()) {
      throw new ValidationException("transactionId không được để trống");
    }
    
    // -- Validate số tiền -- UserPolicy tự ném nếu sai
    UserPolicy.validateWithdrawAmount(amount);

    // -- Kiểm tra user tồn tại
    User user = userDAO.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // -- Kiểm tra balance tổng không vượt giới hạn
    if (!(user instanceof UserMember member)) {
      throw new ValidationException("Tài khoản này không hỗ trợ ví tiền.");
    }

    BigDecimal currentBalance = member.getBalance();
    if (currentBalance.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
      throw new ValidationException(
          "Số dư ví không được âm ");
    }
    
    BigDecimal deltaAmount = amount.negate();
    return userDAO.insertTransactionAndUpdateBalance(
        transactionId, userId, deltaAmount, TransactionType.WITHDRAW, paymentMethod);
  }

  public void settleAuction(int sellerId, int winnerId, BigDecimal finalPrice, 
    int sessionId) throws SQLException {
      // Mã giao dịch cho người mua 
      String sellerTxId = UUID.randomUUID().toString();
      // Mã giao dịch cho người thắng
      String winnerTxId = UUID.randomUUID().toString();

      userDAO.settleAuctionPayment(winnerTxId, sellerTxId, winnerId, sellerId, 
        sessionId, finalPrice, TransactionType.BID_WIN);
    }

  // ═══════════════════════════════════════════════════
  // Truy suất dữ liệu
  // ═══════════════════════════════════════════════════
  public List<TransactionHistoryEntry> getTransactionRecordByUserId(int userId) throws SQLException {
    log.info("Truy suất dữ liệu chuyển tiền của userId=" + userId);
    return transactionDAO.getTransactionsByUserId(userId);
  }
}