package vn.edu.vnu.uet.group8.client.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.common.dto.model.CommentDTO;
import vn.edu.vnu.uet.group8.common.dto.model.ReviewDTO;
import vn.edu.vnu.uet.group8.common.dto.request.ServerRequest;
import vn.edu.vnu.uet.group8.common.enums.ActionType;

/**
 * Gửi thông tin đánh giá Seller lên máy chủ.
 */
public final class RatingService {

    private RatingService() {}

    public static void rateSeller(int itemId, int sellerId, String username, int score, String comment, Consumer<Boolean> onResult, Consumer<String> onFailure) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("itemId", itemId);
        payload.put("sellerId", sellerId);
        payload.put("username", username);
        payload.put("score", score);
        if (comment != null && !comment.isBlank()) {
            payload.put("comment", comment.trim());
        }

        AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.USER_RATE_SELLER, payload, response -> {
                if (response.isSuccess()) {
                    if (onResult != null) onResult.accept(true);
                } else {
                    if (onFailure != null) onFailure.accept(response.getMessage());
                }
            });
    }

    public static void loadSellerReviews(int sellerId, Consumer<List<ReviewDTO>> onResult) {
        Map<String, Integer> payload = Collections.singletonMap("sellerId", sellerId);

        ServerRequest<Map<String, Integer>> request = ServerRequest.<Map<String, Integer>>builder(ActionType.USER_GET_SELLER_REVIEWS)
            .userId(vn.edu.vnu.uet.group8.client.util.SessionManager.getUserId())
            .token(vn.edu.vnu.uet.group8.client.util.SessionManager.getAuthToken())
            .payload(payload).build();

        AuctionClient.getInstance().sendRequest(request, response -> {
            if (response.isSuccess()) {
                List<ReviewDTO> reviews = vn.edu.vnu.uet.group8.common.util.GsonUtil.toList(response.getData(), ReviewDTO.class);
                if (onResult != null) onResult.accept(reviews);
            } else {
                if (onResult != null) onResult.accept(java.util.Collections.emptyList());
            }
        });
    }

    public static void loadSellerComments(int sellerId, Consumer<List<CommentDTO>> onResult) {
        Map<String, Integer> payload = Collections.singletonMap("sellerId", sellerId);

        ServerRequest<Map<String, Integer>> request = ServerRequest.<Map<String, Integer>>builder(ActionType.USER_GET_SELLER_COMMENTS)
            .userId(vn.edu.vnu.uet.group8.client.util.SessionManager.getUserId())
            .token(vn.edu.vnu.uet.group8.client.util.SessionManager.getAuthToken())
            .payload(payload).build();

        AuctionClient.getInstance().sendRequest(request, response -> {
            if (response.isSuccess()) {
                List<CommentDTO> comments = vn.edu.vnu.uet.group8.common.util.GsonUtil.toList(response.getData(), CommentDTO.class);
                if (onResult != null) onResult.accept(comments);
            } else {
                if (onResult != null) onResult.accept(java.util.Collections.emptyList());
            }
        });
    }
}
