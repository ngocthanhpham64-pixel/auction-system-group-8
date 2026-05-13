package vn.edu.vnu.uet.group8.server.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.dto.request.AutoBidRequest;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.dao.BidTransactionDAO.BidHistoryEntry;
import vn.edu.vnu.uet.group8.server.network.ClientHandler;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionService;
import vn.edu.vnu.uet.group8.server.util.GsonUtil;
import vn.edu.vnu.uet.group8.common.dto.request.BidRequest;

import java.util.List;

public class AuctionController {

  private static final Logger log  = LoggerFactory.getLogger(AuctionController.class);
  private static final Gson   GSON = GsonUtil.GSON;

  private final AuctionService auctionService;

  public AuctionController(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  // ── Đặt giá ─────────────────────────────────────────────────────────────

  public ServerResponse placeBid(JsonObject json, String requestId, ClientHandler client) {
    try {
      if (!json.has("payload") || json.get("payload").isJsonNull()) {
        return error("BID_PLACE", requestId, "Thiếu payload đặt giá");
      }
      BidRequest req = GSON.fromJson(json.get("payload"), BidRequest.class);
      if (req == null) {
        return error("BID_PLACE", requestId, "Thiếu payload đặt giá");
      }

      // Đảm bảo bidderId khớp với session — tránh giả mạo userId trong payload
      if (req.getBidderId() != client.getUserId()) {
        return error("BID_PLACE", requestId, "bidderId không khớp với phiên đăng nhập");
      }

      // FIX BUG: auctionService.placeBid() trả về void — không gán vào BidResult
      auctionService.placeBid(req.getBidderId(), req.getItemId(), req.getAmount());

      log.info("BID_PLACE userId={} itemId={} amount={}", client.getUserId(), req.getItemId(), req.getAmount());
      return ServerResponse.reply("BID_PLACE", requestId)
              .success(true)
              .message("Đặt giá thành công")
              .build();

    } catch (Exception e) {
      log.error("BID_PLACE lỗi", e);
      return error("BID_PLACE", requestId, e.getMessage());
    }
  }

  // ── Auto-bid ─────────────────────────────────────────────────────────────

  public ServerResponse setAutoBid(JsonObject json, String requestId, ClientHandler client) {
    try {
      if (!json.has("payload") || json.get("payload").isJsonNull()) {
        return error("BID_AUTO", requestId, "Thiếu payload auto-bid");
      }
      AutoBidRequest req = GSON.fromJson(json.get("payload"), AutoBidRequest.class);
      if (req == null) {
        return error("BID_AUTO", requestId, "Thiếu payload auto-bid");
      }

      auctionService.setAutoBid(req.getItemId(), client.getUserId(), req.getMaxPrice(), req.getStepAmount());

      return ServerResponse.reply("BID_AUTO", requestId)
              .success(true)
              .message("Đặt auto-bid thành công")
              .build();

    } catch (Exception e) {
      log.error("BID_AUTO lỗi", e);
      return error("BID_AUTO", requestId, e.getMessage());
    }
  }

  // ── Lịch sử bid của một item ─────────────────────────────────────────────

  public ServerResponse getBidHistory(JsonObject json, String requestId, ClientHandler client) {
    try {
      if (!json.has("payload") || json.get("payload").isJsonNull()
              || !json.get("payload").isJsonObject()) {
        return error("BID_HISTORY", requestId, "Thiếu payload (cần itemId)");
      }
      int itemId = json.get("payload").getAsJsonObject().get("itemId").getAsInt();

      List<BidHistoryEntry> history = auctionService.getBidHistory(itemId);

      return ServerResponse.reply("BID_HISTORY", requestId)
              .success(true)
              .message("OK")
              .data(history)
              .build();

    } catch (Exception e) {
      log.error("BID_HISTORY lỗi", e);
      return error("BID_HISTORY", requestId, e.getMessage());
    }
  }

  // ── Danh sách bid của user hiện tại ─────────────────────────────────────

  public ServerResponse getUserBids(JsonObject json, String requestId, ClientHandler client) {
    try {
      List<BidHistoryEntry> bids = auctionService.getBidsByUser(client.getUserId());

      return ServerResponse.reply("USER_BIDS", requestId)
              .success(true)
              .message("OK")
              .data(bids)
              .build();

    } catch (Exception e) {
      log.error("USER_BIDS lỗi", e);
      return error("USER_BIDS", requestId, e.getMessage());
    }
  }

  // ── Helper ───────────────────────────────────────────────────────────────

  private ServerResponse error(String action, String requestId, String message) {
    return ServerResponse.reply(action, requestId)
            .success(false).message(message).build();
  }
}