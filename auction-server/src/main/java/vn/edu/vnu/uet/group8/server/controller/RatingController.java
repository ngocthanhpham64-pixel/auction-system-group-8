package vn.edu.vnu.uet.group8.server.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import vn.edu.vnu.uet.group8.common.dto.model.ReviewDTO;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.network.RequestParser;
import vn.edu.vnu.uet.group8.server.service.user.RatingService;

public class RatingController {
  private static final Logger log = LoggerFactory.getLogger(RatingController.class);
  private final RatingService ratingService;

  public RatingController(RatingService ratingService) {
    this.ratingService = ratingService;
  }

  public ServerResponse handleRateSeller(JsonObject request, String requestId, int authenticatedUserId) {
    try {
      JsonObject payload = request.has("payload") && request.get("payload").isJsonObject()
          ? request.getAsJsonObject("payload") : request;

      int sellerId = RequestParser.requireInt(payload, "sellerId");
      int itemId = RequestParser.requireInt(payload, "itemId");
      String username = RequestParser.requireString(payload, "username");
      int score = RequestParser.requireInt(payload, "score");
      String comment = RequestParser.optionalString(payload, "comment");

      ratingService.rateSeller(authenticatedUserId, username, sellerId, itemId, score, comment);

      log.info("User {} đã đánh giá Seller {} với {} sao cho itemId={}", authenticatedUserId, sellerId, score, itemId);
      return ServerResponse.reply("USER_RATE_SELLER", requestId)
          .success(true)
          .message("Đánh giá người bán thành công")
          .build();
    } catch (Exception e) {
      return ServerResponse.replyError("USER_RATE_SELLER", requestId, e.getMessage());
    }
  }

  public ServerResponse handleGetSellerReviews(JsonObject request, String requestId) {
    try {
      JsonObject payload = request.has("payload") && request.get("payload").isJsonObject()
          ? request.getAsJsonObject("payload") : request;
      
      int sellerId = RequestParser.requireInt(payload, "sellerId");
      List<ReviewDTO> reviews = ratingService.getSellerReviews(sellerId);

      return ServerResponse.reply("USER_GET_SELLER_REVIEWS", requestId)
          .success(true)
          .data(reviews)
          .build();
    } catch (Exception e) {
      log.warn("Lỗi lấy danh sách đánh giá người bán sellerId={}", requestId, e);
      return ServerResponse.replyError("USER_GET_SELLER_REVIEWS", requestId, e.getMessage());
    }
  }

  public ServerResponse handleGetSellerComments(JsonObject request, String requestId) {
    try {
      JsonObject payload = request.has("payload") && request.get("payload").isJsonObject()
          ? request.getAsJsonObject("payload") : request;
      
      int sellerId = RequestParser.requireInt(payload, "sellerId");
      List<vn.edu.vnu.uet.group8.common.dto.model.CommentDTO> comments = ratingService.getCommentsForUser(sellerId);

      return ServerResponse.reply("USER_GET_SELLER_COMMENTS", requestId)
          .success(true)
          .data(comments)
          .build();
    } catch (Exception e) {
      log.warn("Lỗi lấy danh sách comment người bán sellerId={}", requestId, e);
      return ServerResponse.replyError("USER_GET_SELLER_COMMENTS", requestId, e.getMessage());
    }
  }
}
