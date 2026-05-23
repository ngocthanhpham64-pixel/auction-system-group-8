package vn.edu.vnu.uet.group8.server.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.BidRecord;
import vn.edu.vnu.uet.group8.common.dto.model.UserBidHistoryDTO;
import vn.edu.vnu.uet.group8.common.dto.request.AutoBidRequest;
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
   * BID_AUTO — proxy bidding.
   */
  public ServerResponse handleAutoBid(JsonObject request, String requestId,
                                      int authenticatedUserId) {
    try {
      AutoBidRequest payload = RequestParser.getPayload(request, AutoBidRequest.class);
      if (payload == null) {
        return ServerResponse.replyError("BID_AUTO", requestId, "Thiếu payload");
      }

      auctionService.placeAutoBid(authenticatedUserId, payload.getItemId(), payload.getMaxPrice());
      return ServerResponse.reply("BID_AUTO", requestId).success(true).message("Thiết lập giá tự động thành công").build();
    } catch (Exception e) {
      log.warn("BID_AUTO thất bại userId={}: {}", authenticatedUserId, e.getMessage());
      return ServerResponse.replyError("BID_AUTO", requestId, "Lỗi: " + e.getMessage());
    }
  }

  // Lịch sử bid của 1 phiên/món hàng (Cho trang chi tiết sản phẩm)
  public ServerResponse handleItemBidHistory(JsonObject request, String requestId) {
    try {
      JsonObject payload = request.has("payload") && request.get("payload").isJsonObject()
          ? request.getAsJsonObject("payload") : request;

      int itemId = RequestParser.requireInt(payload, "itemId");
      
      // Trả về List<BidRecord> chuẩn form FE
      List<BidRecord> history = auctionService.getItemBidHistory(itemId);
      return ServerResponse.reply("BID_HISTORY", requestId)
                              .success(true)
                              .data(history)
                              .build();
    } catch (Exception e) {
      log.error("Lỗi khi lấy lịch sử phiên={}: {}", request, e.getMessage());
      return ServerResponse.replyError("BID_HISTORY", requestId, "Lỗi server: " + e.getMessage());
    }
  }

  // Lịch sử bid của chính User (Cho trang Quản lý tài khoản)
  public ServerResponse handleUserBidHistory(JsonObject request, String requestId, int authenticatedUserId) {
    try {
      // Trả về List<UserBidHistoryDTO>
      List<UserBidHistoryDTO> userBidHistory = auctionService.getUserBidHistory(authenticatedUserId);
      return ServerResponse.reply("USER_BIDS", requestId)
                              .success(true)
                              .data(userBidHistory)
                              .build();
    } catch (Exception e) {
      log.error("Lỗi lấy lịch sử đấu giá userId={}: {}", authenticatedUserId, e.getMessage());
      return ServerResponse.replyError("USER_BIDS", requestId, "Lỗi server: " + e.getMessage());
    }
  }
}
