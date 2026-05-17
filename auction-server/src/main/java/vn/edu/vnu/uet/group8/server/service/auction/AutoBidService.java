package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.exception.UserNotFoundException;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

public class AutoBidService {
  private static final Logger logger = LoggerFactory.getLogger(AutoBidService.class);
  private final AutoBidDAO autoBidDAO;
  private final BidValidator validator;
  private final BidProcessor processor;
  private final UserDAO userDAO;

  public AutoBidService(AutoBidDAO autoBidDAO, BidValidator validator, BidProcessor processor, UserDAO userDAO) {
    this.autoBidDAO = autoBidDAO;
    this.validator = validator;
    this.processor = processor;
    this.userDAO = userDAO;
  }

  public void configureAutoBid(int userId, int sessionId, BigDecimal maxPrice) throws SQLException {
    User user = userDAO.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    if (!(user instanceof UserMember member)) {
        throw new ValidationException("Tài khoản không được phép đấu giá.");
    }
    if (member.getBalance().compareTo(maxPrice) < 0) {
        throw new ValidationException("Số dư khả dụng không đủ để thiết lập Auto-bid (" + maxPrice + " VND)");
    }

    AutoBidConfig config = AutoBidConfig.builder()
                                        .userId(userId)
                                        .sessionId(sessionId)
                                        .maxPrice(maxPrice)
                                        .build();
    autoBidDAO.saveConfig(config);
    logger.info("AutoBid: User {} đã cài maxPrice {} cho session {}", userId, maxPrice, sessionId);
  }

  public BidResult resolveAutoBids(AuctionSession session) throws SQLException {
    List<AutoBidConfig> configs = autoBidDAO.findActiveBySession(session.getId());
    if (configs.isEmpty()) return null;

    AutoBidConfig top1 = configs.get(0);
    Integer currentLeaderId = session.getHighestBidderId();
    BigDecimal currentPrice = session.getCurrentPrice();
    BigDecimal priceToBeat = currentPrice;

    boolean isTop1Winning = currentLeaderId != null && currentLeaderId.equals(top1.getUserId());

    // Nếu có đối thủ Auto-bid thứ 2 mạnh hơn giá hiện tại, rào cản sẽ là mức giá của họ
    if (configs.size() > 1 && configs.get(1).getMaxPrice().compareTo(currentPrice) > 0) {
      priceToBeat = configs.get(1).getMaxPrice();
    } else if (isTop1Winning) {
      // Nếu không có đối thủ challenge và Top 1 đang thắng -> Dừng, không tự bid đè lên mình
      return null;
    }

    BigDecimal nextBid = (session.getBidCount() == 0 && priceToBeat.compareTo(session.getStartingPrice()) == 0) 
                         ? session.getStartingPrice() : BidValidator.calculateMinNextBid(priceToBeat);
    if (nextBid.compareTo(top1.getMaxPrice()) > 0) nextBid = top1.getMaxPrice();
    if (nextBid.compareTo(currentPrice) <= 0 && currentLeaderId != null) return null;

    try {
        BidContext context = validator.validate(top1.getUserId(), session.getId(), nextBid);
        BidResult result = processor.process(context);
        logger.info("Toán học Auto-bid: User {} tự động thắng với giá {}", top1.getUserId(), nextBid);
        return result;
    } catch (ValidationException | BidTransactionDAO.InsufficientBalanceException e) {
        logger.warn("Auto-bid thất bại cho user {} (Lý do: {}). Đang hủy cấu hình.", top1.getUserId(), e.getMessage());
        autoBidDAO.deactivate(top1.getId());
        return resolveAutoBids(session); // Đệ quy tìm người mạnh thứ 2 nếu Top 1 hết tiền
    }
  }
}
