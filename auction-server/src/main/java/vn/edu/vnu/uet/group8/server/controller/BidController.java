package vn.edu.vnu.uet.group8.server.controller;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.dto.request.BidRequest;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.network.RequestParser;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;

/**
 * Controller cho domain Bid: BID_PLACE, BID_AUTO, BID_HISTORY.
 *
 * <p>Note: BID_AUTO và BID_HISTORY hiện chưa có service tương ứng
 * (AuctionService chỉ có placeBid). Khi service ready → mở rộng.
 */
public class BidController {
  private static final Logger log = LoggerFactory.getLogger(BidController.class);

  private final AuctionService auctionService;

  public BidController(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  /**
   * BID_PLACE — user đặt giá lên một phiên.
   * userId lấy từ session, KHÔNG tin payload.bidderId (có thể bị giả).
   *
   * @param authenticatedUserId userId đã được verify qua token
   */
  public ServerResponse handlePlaceBid(JsonObject request, String requestId,
                                       int authenticatedUserId) {
    try {
      BidRequest payload = RequestParser.getPayload(request, BidRequest.class);
      if (payload == null) {
        return ServerResponse.replyError("BID_PLACE", requestId, "Thiếu payload");
      }

      // Bidder lấy từ session — không tin client
      auctionService.placeBid(
          authenticatedUserId,
          payload.getItemId(),     // service map sang sessionId qua DAO
          payload.getAmount());

      log.info("User {} bid {} cho item {}",
          authenticatedUserId, payload.getAmount(), payload.getItemId());

      return ServerResponse.reply("BID_PLACE", requestId)
          .success(true)
          .message("Đặt giá thành công")
          .build();

    } catch (Exception e) {
      log.warn("BID_PLACE thất bại userId={}: {}", authenticatedUserId, e.getMessage());
      return ServerResponse.replyError("BID_PLACE", requestId,
          "Đặt giá thất bại: " + e.getMessage());
    }
  }

  /**
   * BID_AUTO — placeholder cho proxy bidding.
   * Service chưa implement → trả "not supported".
   */
  public ServerResponse handleAutoBid(JsonObject request, String requestId,
                                      int authenticatedUserId) {
    log.debug("BID_AUTO chưa hỗ trợ — userId={}", authenticatedUserId);
    return ServerResponse.replyError("BID_AUTO", requestId,
        "Tính năng đặt giá tự động chưa được hỗ trợ");
  }

  /**
   * BID_HISTORY — placeholder.
   * Khi BidTransactionDAO có findByItemId() / findByUserId() → implement.
   */
  public ServerResponse handleBidHistory(JsonObject request, String requestId,
                                         int authenticatedUserId) {
    log.debug("BID_HISTORY chưa hỗ trợ — userId={}", authenticatedUserId);
    return ServerResponse.replyError("BID_HISTORY", requestId,
        "Lịch sử đặt giá chưa được hỗ trợ");
  }
}
