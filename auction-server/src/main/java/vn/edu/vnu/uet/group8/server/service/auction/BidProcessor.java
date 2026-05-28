package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.BidAction;

public class BidProcessor {

  private static final Logger logger = LoggerFactory.getLogger(BidProcessor.class);

  private final BidTransactionDAO bidTransactionDAO;
  private final AutoBidDAO autoBidDAO;
  private final AuctionSessionDAO sessionDAO;

  public BidProcessor(BidTransactionDAO bidTransactionDAO, AutoBidDAO autoBidDAO, AuctionSessionDAO sessionDAO) {
    this.bidTransactionDAO = bidTransactionDAO;
    this.autoBidDAO = autoBidDAO;
    this.sessionDAO = sessionDAO;
  }

  public BidResult resolveFight(BidContext context) throws SQLException {
    int challengerId = context.getBidder().getId();
    BigDecimal maxChallenger = context.getBidAmount();
    int sessionId = context.getAuctionSession().getId();

    logger.info("resolveFight bắt đầu: challenger={}, maxChallenger={}, sessionId={}", challengerId, maxChallenger, sessionId);

    try (Connection conn = vn.edu.vnu.uet.group8.server.dao.DatabaseConnection.getInstance().getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Bước 2: Khóa dòng và Nạp dữ liệu lên RAM
        AuctionSession lockedSession = sessionDAO.lockSessionForUpdate(conn, sessionId)
            .orElseThrow(() -> new ValidationException("Phiên đấu giá không tồn tại hoặc đã đóng"));

        BigDecimal currentPrice = lockedSession.getCurrentPrice();
        Integer leaderIdObj = lockedSession.getHighestBidderId();
        int leaderId = leaderIdObj != null ? leaderIdObj : 0;
        
        // Kiểm tra bước giá lại sau khi có lock mới nhất
        BigDecimal minNextBid = lockedSession.getBidCount() == 0 ? lockedSession.getStartingPrice() : BidValidator.calculateMinNextBid(currentPrice);
        if (maxChallenger.compareTo(minNextBid) < 0) {
            throw new ValidationException(String.format("Giá đặt tối thiểu là %s VND", minNextBid));
        }

        BigDecimal maxLeader = currentPrice;
        AutoBidConfig leaderConfig = null;

        if (leaderId != 0) {
            List<AutoBidConfig> configs = autoBidDAO.findActiveBySession(conn, sessionId);
            for (AutoBidConfig c : configs) {
                if (c.getUserId() == leaderId) {
                    leaderConfig = c;
                    maxLeader = c.getMaxPrice();
                    break;
                }
            }
        }

        BigDecimal step = BidValidator.calculateMinNextBid(currentPrice).subtract(currentPrice);
        
        BigDecimal newPrice;
        int winnerId;
        List<BidAction> actions = new ArrayList<>();
        boolean leaderChanged;

        // Kịch bản nếu chưa có Leader (First bid)
        if (leaderId == 0) {
            winnerId = challengerId;
            newPrice = lockedSession.getBidCount() == 0 ? lockedSession.getStartingPrice() : currentPrice;
            leaderChanged = true;
            actions.add(new BidAction(challengerId, newPrice));
        } else {
            // Bước 3: Phân định Thắng - Thua
            int compare = maxChallenger.compareTo(maxLeader);
            if (compare < 0) {
                // Kịch bản 1: Challenger yếu hơn Leader
                winnerId = leaderId;
                leaderChanged = false;
                BigDecimal defenseTarget = maxChallenger.add(step);
                newPrice = defenseTarget.compareTo(maxLeader) > 0 ? maxLeader : defenseTarget;
                actions.add(new BidAction(challengerId, maxChallenger));
                actions.add(new BidAction(leaderId, newPrice));
            } else if (compare > 0) {
                // Kịch bản 2: Challenger mạnh hơn
                winnerId = challengerId;
                leaderChanged = true;
                BigDecimal takeoverTarget = maxLeader.add(step);
                newPrice = takeoverTarget.compareTo(maxChallenger) > 0 ? maxChallenger : takeoverTarget;
                actions.add(new BidAction(leaderId, maxLeader));
                actions.add(new BidAction(challengerId, newPrice));
            } else {
                // Kịch bản 3: Bằng giá (FCFS)
                winnerId = leaderId;
                leaderChanged = false;
                newPrice = maxLeader;
                actions.add(new BidAction(challengerId, maxChallenger));
                actions.add(new BidAction(leaderId, maxLeader));
            }
        }

        // Bước 4: Ví tiền
        if (leaderChanged) {
            if (leaderId != 0) {
                bidTransactionDAO.refundBidder(conn, leaderId, maxLeader, sessionId);
            }
            boolean holdSuccess = bidTransactionDAO.holdBalance(conn, challengerId, maxChallenger, sessionId);
            if (!holdSuccess) {
                throw new ValidationException("Số dư không đủ để thực hiện giao dịch.");
            }
        }

        // Bước 5: Lưu vết Lịch sử và Database Commit
        long lastTransactionId = -1;
        for (BidAction action : actions) {
            lastTransactionId = bidTransactionDAO.insertBidTransaction(conn, action.bidderId(), sessionId, action.amount());
        }

        int affected = bidTransactionDAO.updateSessionAfterFight(conn, sessionId, newPrice, winnerId, actions.size());
        if (affected == 0) {
            throw new ValidationException("Không thể cập nhật phiên đấu giá.");
        }
        
        int totalBids = bidTransactionDAO.countByItemInTx(conn, sessionId);

        // Cập nhật cấu hình Autobid
        if (leaderChanged) {
            if (leaderConfig != null) {
                autoBidDAO.deactivate(conn, leaderConfig.getId());
            }
            saveActiveConfig(conn, challengerId, sessionId, maxChallenger);
        }

        conn.commit();

        // Đồng bộ Memory để trả về event bus
        if (newPrice.compareTo(lockedSession.getCurrentPrice()) > 0) {
            lockedSession.raiseCurrentPrice(newPrice);
        }
        for (int i = 0; i < actions.size(); i++) {
            lockedSession.incrementBidCount();
        }
        lockedSession.setHighestBidderId(winnerId);
        
        // Đồng bộ vào context để các hàm bên ngoài có giá mới nhất
        if (lockedSession != context.getAuctionSession()) {
            if (newPrice.compareTo(context.getAuctionSession().getCurrentPrice()) > 0) {
                context.getAuctionSession().raiseCurrentPrice(newPrice);
            }
            for (int i = 0; i < actions.size(); i++) {
                context.getAuctionSession().incrementBidCount();
            }
            context.getAuctionSession().setHighestBidderId(winnerId);
        }

        if (!leaderChanged) {
            // Ném ngoại lệ y như code cũ
            throw new ValidationException("Bạn đã bị vượt giá ngay lập tức bởi hệ thống phòng thủ.");
        }

        return new BidResult(
            lastTransactionId, lockedSession.getItemId(), sessionId, winnerId,
            context.getBidder().getUsername(), newPrice, totalBids, lockedSession.getEndTime(), leaderId == 0 ? null : leaderId);
            
      } catch (Exception e) {
        conn.rollback();
        throw e;
      }
    }
  }

  private void saveActiveConfig(Connection conn, int userId, int sessionId, BigDecimal maxPrice) throws SQLException {
      AutoBidConfig config = AutoBidConfig.builder()
                                          .userId(userId)
                                          .sessionId(sessionId)
                                          .maxPrice(maxPrice)
                                          .build();
      autoBidDAO.saveConfig(conn, config);
  }
}