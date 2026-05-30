package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.AutoBidConfig;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.AuctionSessionDAO;
import vn.edu.vnu.uet.group8.server.dao.AutoBidDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;

public class HybridBidExecutor {
    private static final Logger logger = LoggerFactory.getLogger(HybridBidExecutor.class);

    private final BidTransactionDAO bidTransactionDAO;
    private final AutoBidDAO autoBidDAO;
    private final AuctionSessionDAO sessionDAO;

    public HybridBidExecutor(BidTransactionDAO bidTransactionDAO, AutoBidDAO autoBidDAO, AuctionSessionDAO sessionDAO) {
        this.bidTransactionDAO = bidTransactionDAO;
        this.autoBidDAO = autoBidDAO;
        this.sessionDAO = sessionDAO;
    }

    public BidResult execute(BidContext context, boolean isAuto) throws SQLException {
        int challengerId = context.getBidder().getId();
        BigDecimal newMaxPrice = context.getBidAmount();
        int sessionId = context.getAuctionSession().getId();

        logger.info("HybridBidExecutor bắt đầu: challenger={}, maxPrice={}, isAuto={}, sessionId={}", 
            challengerId, newMaxPrice, isAuto, sessionId);

        try (Connection conn = vn.edu.vnu.uet.group8.server.dao.DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Bước 2: Khóa dòng dữ liệu
                AuctionSession lockedSession = sessionDAO.lockSessionForUpdate(conn, sessionId)
                    .orElseThrow(() -> new ValidationException("Phiên đấu giá không tồn tại hoặc đã đóng"));

                BigDecimal currentPrice = lockedSession.getCurrentPrice();
                Integer previousLeaderIdObj = lockedSession.getHighestBidderId();
                int previousLeaderId = previousLeaderIdObj != null ? previousLeaderIdObj : 0;
                
                // Bước 1: Kiểm tra tính hợp lệ (bước giá)
                BigDecimal minNextBid = lockedSession.getBidCount() == 0 ? lockedSession.getStartingPrice() : BidValidator.calculateMinNextBid(currentPrice);
                if (newMaxPrice.compareTo(minNextBid) < 0) {
                    throw new ValidationException(String.format("Giá đặt tối thiểu là %s VND", minNextBid));
                }

                BigDecimal step = BidValidator.calculateMinNextBid(currentPrice).subtract(currentPrice);

                // Bước 3: Khởi tạo Hàng đợi ưu tiên
                PriorityQueue<BidCommand> pq = new PriorityQueue<>();

                // Bước 4: Nạp dữ liệu vào hàng đợi và tính toán previousLeaderMaxPrice
                List<AutoBidConfig> configs = autoBidDAO.findActiveBySession(conn, sessionId);
                
                BigDecimal previousLeaderMaxPrice = BigDecimal.ZERO;
                if (previousLeaderId != 0) {
                    previousLeaderMaxPrice = currentPrice; // Mặc định ít nhất là currentPrice
                    for (AutoBidConfig c : configs) {
                        if (c.getUserId() == previousLeaderId && c.getMaxPrice().compareTo(currentPrice) > 0) {
                            previousLeaderMaxPrice = c.getMaxPrice();
                            break;
                        }
                    }
                }

                for (AutoBidConfig c : configs) {
                    if (c.getUserId() == challengerId) {
                        // Bỏ qua config cũ của người đang đặt để đè bằng lệnh mới
                        continue;
                    }
                    pq.add(new BidCommand(c.getUserId(), c.getMaxPrice(), c.getCreatedAt().toEpochMilli(), true));
                }

                // Nếu previous leader không phải challenger, và họ đang giữ đỉnh bằng lệnh thủ công (không có autobid mạnh hơn currentPrice)
                // thì nạp lệnh thủ công đó vào PQ
                if (previousLeaderId != 0 && previousLeaderId != challengerId) {
                    boolean hasStrongConfig = false;
                    for (AutoBidConfig c : configs) {
                        if (c.getUserId() == previousLeaderId && c.getMaxPrice().compareTo(currentPrice) >= 0) {
                            hasStrongConfig = true;
                            break;
                        }
                    }
                    if (!hasStrongConfig) {
                        pq.add(new BidCommand(previousLeaderId, currentPrice, 0L, false));
                    }
                }

                // Thêm lệnh mới vào hàng đợi
                pq.add(new BidCommand(challengerId, newMaxPrice, System.currentTimeMillis(), isAuto));

                // Bước 5: Phân định thắng thua và Tính toán giá chốt
                BidCommand top1 = pq.poll();
                
                // Tìm đối thủ mạnh nhất (khác top1.userId)
                BidCommand opponent = null;
                BidCommand top2_original = pq.peek(); // Giữ lại người đứng thứ 2 thực sự để log lịch sử thất bại
                BidCommand next = pq.poll();
                while (next != null) {
                    if (next.userId() != top1.userId()) {
                        opponent = next;
                        break;
                    }
                    next = pq.poll();
                }

                BigDecimal newPrice;
                if (!top1.isAuto()) {
                    // Nhánh 1: Thủ công -> Vọt thẳng trần
                    newPrice = top1.maxPrice();
                } else {
                    // Nhánh 2: Tự động
                    if (opponent == null) {
                        // Không có đối thủ
                        if (previousLeaderId == top1.userId()) {
                            newPrice = currentPrice; // Giữ nguyên giá nếu tự update config
                        } else {
                            newPrice = lockedSession.getBidCount() == 0 ? lockedSession.getStartingPrice() : currentPrice;
                        }
                    } else {
                        // Có đối thủ
                        if (top1.maxPrice().compareTo(opponent.maxPrice()) == 0) {
                            newPrice = top1.maxPrice();
                        } else {
                            BigDecimal defenseTarget = opponent.maxPrice().add(step);
                            newPrice = defenseTarget.compareTo(top1.maxPrice()) > 0 ? top1.maxPrice() : defenseTarget;
                        }
                    }
                }

                // Đảm bảo newPrice không tụt lùi so với giá hiện tại
                if (lockedSession.getBidCount() > 0 && newPrice.compareTo(currentPrice) < 0) {
                    newPrice = currentPrice;
                }

                int winnerId = top1.userId();
                boolean leaderChanged = (winnerId != previousLeaderId);

                // Bước 6: Quản lý dòng tiền ví tài chính
                // Luôn giải phóng tiền của previous leader (kể cả khi họ tự đẩy giá của chính mình)
                if (previousLeaderId != 0 && previousLeaderMaxPrice.compareTo(BigDecimal.ZERO) > 0) {
                    bidTransactionDAO.refundBidder(conn, previousLeaderId, previousLeaderMaxPrice, sessionId);
                }
                
                // Luôn đóng băng tiền của top 1 với maxPrice mới
                boolean holdSuccess = bidTransactionDAO.holdBalance(conn, winnerId, top1.maxPrice(), sessionId);
                if (!holdSuccess) {
                    throw new ValidationException("Số dư không đủ để thực hiện giao dịch.");
                }

                // Bước 7: Ghi nhận lịch sử và Hoàn tất
                long lastTransactionId = -1;
                
                // Ghi nhận Top 2 (Thất bại) - Lấy người thua cuộc thật sự gần nhất
                if (top2_original != null && top2_original.userId() != top1.userId()) {
                    // Status = 'OUTBID' hoặc 'FAILED'
                    bidTransactionDAO.insertBidTransaction(conn, top2_original.userId(), sessionId, top2_original.maxPrice(), "OUTBID");
                    
                    // Vô hiệu hóa cấu hình autobid của người thua
                    autoBidDAO.deactivateOutbidConfigs(conn, sessionId, newPrice, winnerId);
                }
                
                // Ghi nhận Top 1 (Dẫn đầu)
                lastTransactionId = bidTransactionDAO.insertBidTransaction(conn, winnerId, sessionId, newPrice, "LEADER");

                // Cập nhật cấu hình autobid cho Top 1 nếu là isAuto
                if (top1.isAuto()) {
                    // Nếu là lệnh mới tự động hoặc leader changed, ghi đè config
                    saveActiveConfig(conn, winnerId, sessionId, top1.maxPrice());
                }

                // Cập nhật session
                int bidsToAdd = (top2_original != null && top2_original.userId() != top1.userId()) ? 2 : 1;
                int affected = bidTransactionDAO.updateSessionAfterFight(conn, sessionId, newPrice, winnerId, bidsToAdd);
                if (affected == 0) {
                    throw new ValidationException("Không thể cập nhật phiên đấu giá.");
                }
                
                int totalBids = bidTransactionDAO.countByItemInTx(conn, sessionId);

                conn.commit();

                // Đồng bộ Memory để trả về event bus
                if (newPrice.compareTo(lockedSession.getCurrentPrice()) > 0) {
                    lockedSession.raiseCurrentPrice(newPrice);
                }
                lockedSession.incrementBidCount(); // Tăng 1 hoặc 2 tùy số record ghi nhận
                if (top2_original != null && top2_original.userId() != top1.userId()) lockedSession.incrementBidCount();
                lockedSession.setHighestBidderId(winnerId);
                
                // Đồng bộ vào context
                if (lockedSession != context.getAuctionSession()) {
                    if (newPrice.compareTo(context.getAuctionSession().getCurrentPrice()) > 0) {
                        context.getAuctionSession().raiseCurrentPrice(newPrice);
                    }
                    context.getAuctionSession().incrementBidCount();
                    if (top2_original != null && top2_original.userId() != top1.userId()) context.getAuctionSession().incrementBidCount();
                    context.getAuctionSession().setHighestBidderId(winnerId);
                }



                return new BidResult(
                    lastTransactionId, lockedSession.getItemId(), sessionId, winnerId,
                    context.getBidder().getUsername(), newPrice, totalBids, lockedSession.getEndTime(), previousLeaderId == 0 ? null : previousLeaderId);

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
