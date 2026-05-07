package vn.edu.vnu.uet.group8.server.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.server.dao.ItemDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

/**
 * Tầng Service xử lý nghiệp vụ cốt lõi của hệ thống Đấu giá.
 * Áp dụng nghiêm ngặt các ràng buộc thực thể (Domain Constraints).
 */
public class AuctionService1 {

  // Các DAO (Thủ kho) để giao tiếp với DB
  private final ItemDAO itemDao;
  private final UserDAO userDao;

  public AuctionService1(ItemDAO ItemDao, UserDAO UserDao) {
    this.itemDao = Objects.requireNonNull(ItemDao);
    this.userDao = Objects.requireNonNull(UserDao);
  }

  /**
   * Xử lý luồng nghiệp vụ "Đặt giá" (Place a Bid).
   * Từ khóa 'synchronized' giúp chống Race Condition: Nếu 2 request cùng gọi hàm này 
   * trên cùng một đối tượng món hàng, hệ thống sẽ xếp hàng xử lý tuần tự.
   *
   * @param bidderId ID của người đặt giá
   * @param itemId ID của món hàng
   * @param bidAmount Số tiền đặt giá
   * @throws IllegalArgumentException nếu vi phạm bất kỳ Business Logic nào
   */
  public synchronized void placeBid(int bidderId, int itemId, BigDecimal bidAmount) {
    
    // 1. Lấy dữ liệu gốc từ DB (Data Retrieval)
    User bidder = UserDAO.findById(bidderId);
    Item item = ItemDAO.findById(itemId);

    if (bidder == null || item == null) {
      throw new IllegalArgumentException("Người dùng hoặc Món hàng không tồn tại.");
    }

    // =====================================================================
    // KIỂM TRA RÀNG BUỘC THỰC THỂ (DOMAIN CONSTRAINTS)
    // =====================================================================

    // 2. Ràng buộc Quan hệ (Relational): Không tự đấu giá hàng của mình
    if (item.getOwnerId() == bidder.getId()) {
      throw new IllegalArgumentException("Bạn không thể tự đặt giá cho món hàng của mình.");
    }

    // 3. Ràng buộc Trạng thái (Stateful): Hàng phải đang mở bán
    if (!"ACTIVE".equals(item.getStatus())) {
      throw new IllegalArgumentException("Món hàng này hiện không trong trạng thái đấu giá.");
    }

    // 4. Ràng buộc Thời gian (Temporal): Chưa quá hạn
    if (Instant.now().isAfter(item.getEndTime())) {
      throw new IllegalArgumentException("Phiên đấu giá cho món hàng này đã kết thúc.");
    }

    // 5. Ràng buộc Miền giá trị (Value Range): Giá đặt phải hợp lệ
    BigDecimal minimumRequiredBid = item.getCurrentPrice().add(item.getStepPrice());
    if (bidAmount.compareTo(minimumRequiredBid) < 0) {
      throw new IllegalArgumentException(
          String.format("Giá đặt phải lớn hơn hoặc bằng %s", minimumRequiredBid.toString())
      );
    }

    // 6. Ràng buộc Tài chính: Số dư ví phải đủ để tạm giữ
    if (bidder.getBalance().compareTo(bidAmount) < 0) {
      throw new IllegalArgumentException("Số dư trong ví không đủ để thực hiện đặt giá.");
    }

    // =====================================================================
    // THỰC THI NGHIỆP VỤ & QUẢN LÝ GIAO DỊCH (TRANSACTION)
    // =====================================================================

    try {
      // Bắt đầu Transaction (Mã giả - tùy thuộc vào thư viện DB bạn dùng)
      // dbConnection.setAutoCommit(false);

      // Bước A: Hoàn tiền cho người đang giữ giá cao nhất (nếu có)
      if (item.getHighestBidderId() != null) {
        User previousBidder = UserDAO.findById(item.getHighestBidderId());
        if (previousBidder != null) {
          BigDecimal refundedBalance = previousBidder.getBalance().add(item.getCurrentPrice());
          UserDAO.updateBalance(previousBidder.getId(), refundedBalance);
        }
      }

      // Bước B: Trừ tiền (tạm giữ) của người vừa đặt giá thành công
      BigDecimal newBalance = bidder.getBalance().subtract(bidAmount);
      UserDAO.updateBalance(bidder.getId(), newBalance);

      // Bước C: Cập nhật thông tin món hàng với kỷ lục mới
      ItemDAO.updateHighestBid(item.getId(), bidder.getId(), bidAmount);

      // Hoàn tất Transaction
      // dbConnection.commit();

    } catch (Exception e) {
      // Nếu có bất kỳ lỗi gì (đứt mạng DB), hoàn tác mọi thay đổi
      // dbConnection.rollback();
      throw new RuntimeException("Lỗi hệ thống khi xử lý giao dịch. Vui lòng thử lại sau.", e);
    }
  }
}