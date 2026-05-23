package vn.edu.vnu.uet.group8.server.service.auction;

import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.BidExecutionResult;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.LeaderInfo;

/**
 * Điều phối luồng xử lý một lần đặt giá đã được validate.
 *
 * <p>Không chứa SQL, không biết tên cột, không quản lý Connection.
 * Toàn bộ thao tác DB được ủy quyền cho {@link BidTransactionDAO}.
 *
 * <p>Lock được quản lý bên ngoài tại {@link AuctionService} --
 * bao ngoài cả validate lẫn process để đảm bảo atomicity.
 */
public class BidProcessor {

  private static final Logger logger = LoggerFactory.getLogger(BidProcessor.class);

  private final BidTransactionDAO bidTransactionDAO;

  public BidProcessor(BidTransactionDAO bidTransactionDAO) {
    this.bidTransactionDAO = bidTransactionDAO;
  }

  /**
   * Thực thi đặt giá.
   *
   * <p>Luồng:
   * <ol>
   *   <li>Tìm người đang dẫn đầu -- cần để hoàn tiền
   *   <li>Gọi {@link BidTransactionDAO#executeBid} -- toàn bộ atomic tại DAO
   *   <li>Sync Entity trong memory -- không query DB lại
   *   <li>Build và trả {@link BidResult}
   * </ol>
   *
   * @param context kết quả từ {@link BidValidator} -- đã validate hợp lệ
   * @return {@link BidResult} để {@link AuctionService} broadcast và build response
   * @throws SQLException nếu lỗi DB
   */
  public BidResult process(BidContext context) throws SQLException {
    int bidderId = context.getBidder().getId();
    int itemId = context.getItem().getId();
    int sessionId = context.getAuctionSession().getId();

    logger.info("Bắt đầu process bid: bidderId={}, itemId={}, auctionSessionId={}, amount={}",
        bidderId, itemId, sessionId, context.getBidAmount());

    // -- Bước 1: Tìm leader hiện tại trước khi update
    // Cần biết ai đang dẫn đầu để hoàn tiền đúng người
    Optional<LeaderInfo> prevLeader =
        bidTransactionDAO.findCurrentLeader(sessionId);

    // -- Bước 2: Ủy quyền toàn bộ DB operation cho DAO
    // BidProcessor không biết gì về SQL hay Connection
    BidExecutionResult result = bidTransactionDAO.executeBid(
        bidderId,
        sessionId,
        context.getBidAmount(),
        prevLeader);

    // -- Bước 3: Sync Entity trong memory
    // Tránh query DB lại -- cập nhật trực tiếp trên object đã có
    context.getAuctionSession().raiseCurrentPrice(context.getBidAmount());
    context.getAuctionSession().incrementBidCount();
    context.getAuctionSession().setHighestBidderId(bidderId);

    logger.info("Process bid thành công: txId={}, itemId={}, auctionSessionId={}, newPrice={}",
        result.transactionId(), itemId, context.getAuctionSession().getId(), context.getBidAmount());

    // -- Bước 4: Build BidResult để AuctionService dùng
    return new BidResult(
        result.transactionId(),
        itemId,
        context.getAuctionSession().getId(),
        bidderId,
        context.getBidder().getUsername(),
        context.getBidAmount(),
        result.totalBids(),
        context.getAuctionSession().getEndTime(),
        prevLeader.map(LeaderInfo::bidderId).orElse(null));
  }
}