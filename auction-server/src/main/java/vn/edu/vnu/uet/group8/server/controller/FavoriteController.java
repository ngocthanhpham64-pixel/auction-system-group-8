package vn.edu.vnu.uet.group8.server.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.network.RequestParser;
import vn.edu.vnu.uet.group8.server.service.item.FavoriteService;

public class FavoriteController {
  private static final Logger log = LoggerFactory.getLogger(FavoriteController.class);
  private final FavoriteService favoriteService;

  public FavoriteController(FavoriteService favoriteService) {
    this.favoriteService = favoriteService;
  }

  public ServerResponse handleGetList(String requestId, int userId) {
    try {
      List<AuctionItemDTO> items = favoriteService.getFavoriteItems(userId);
      return ServerResponse.reply("FAVORITE_LIST", requestId)
          .success(true)
          .data(items)
          .build();
    } catch (Exception e) {
      log.error("Lỗi lấy danh sách yêu thích cho userId={}", userId, e);
      return ServerResponse.replyError("FAVORITE_LIST", requestId, "Lỗi máy chủ: " + e.getMessage());
    }
  }

  public ServerResponse handleAdd(JsonObject request, String requestId, int userId) {
    try {
      // Hỗ trợ lấy itemId từ payload hoặc trực tiếp từ root request
      JsonObject payload = request.has("payload") ? request.getAsJsonObject("payload") : request;
      int itemId = RequestParser.requireInt(payload, "itemId");
      
      favoriteService.addFavorite(userId, itemId);
      return ServerResponse.reply("FAVORITE_ADD", requestId)
          .success(true)
          .message("Đã thêm vào danh sách yêu thích")
          .build();
    } catch (Exception e) {
      return ServerResponse.replyError("FAVORITE_ADD", requestId, e.getMessage());
    }
  }

  public ServerResponse handleRemove(JsonObject request, String requestId, int userId) {
    try {
      JsonObject payload = request.has("payload") ? request.getAsJsonObject("payload") : request;
      int itemId = RequestParser.requireInt(payload, "itemId");
      
      favoriteService.removeFavorite(userId, itemId);
      return ServerResponse.reply("FAVORITE_REMOVE", requestId)
          .success(true).message("Đã xóa khỏi danh sách yêu thích").build();
    } catch (Exception e) {
      return ServerResponse.replyError("FAVORITE_REMOVE", requestId, e.getMessage());
    }
  }
}
