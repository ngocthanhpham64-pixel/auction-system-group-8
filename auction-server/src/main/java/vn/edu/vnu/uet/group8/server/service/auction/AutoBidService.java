package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

public class AutoBidService {
  private static final Logger logger = LoggerFactory.getLogger(AutoBidService.class);
  private final AutoBidDAO autoBidDAO;
  private final BidValidator validator;
  private final HybridBidExecutor executor;
  private final UserDAO userDAO;

  public AutoBidService(AutoBidDAO autoBidDAO, BidValidator validator, HybridBidExecutor executor, UserDAO userDAO) {
    this.autoBidDAO = autoBidDAO;
    this.validator = validator;
    this.executor = executor;
    this.userDAO = userDAO;
  }

  public boolean hasActiveAutoBid(int userId, int sessionId) throws SQLException {
      return autoBidDAO.hasActiveAutoBid(userId, sessionId);
  }

  public void cancelAutoBid(int userId, int sessionId) throws SQLException {
      List<AutoBidConfig> configs = autoBidDAO.findActiveBySession(sessionId);
      for (AutoBidConfig config : configs) {
          if (config.getUserId() == userId) {
              autoBidDAO.deactivate(config.getId());
          }
      }
  }

  /**
   * Lưu cấu hình Autobid vào DB.
   * Logic kiểm tra số dư và giải quyết thắng thua sẽ được thực hiện tại HybridBidExecutor.execute()
   */
  public void saveConfig(int userId, int sessionId, BigDecimal maxPrice) throws SQLException {
      // Lưu cấu hình vào DB (deactivate config cũ nếu có)
      AutoBidConfig config = AutoBidConfig.builder()
                                          .userId(userId)
                                          .sessionId(sessionId)
                                          .maxPrice(maxPrice)
                                          .build();
      autoBidDAO.saveConfig(config);
      logger.info("AutoBid: User {} đã cài maxPrice {} cho session {}", userId, maxPrice, sessionId);
  }
}
