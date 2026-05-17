package vn.edu.vnu.uet.group8.client.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.common.enums.ActionType;

/**
 * Gửi thông tin đánh giá Seller lên máy chủ.
 */
public final class RatingService {

    private RatingService() {}

    public static void rateSeller(int sellerId, int score, String comment, Consumer<Boolean> onResult, Consumer<String> onFailure) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sellerId", sellerId);
        payload.put("score", score);
        if (comment != null && !comment.isBlank()) {
            payload.put("comment", comment.trim());
        }

        AuctionClient.getInstance().sendAuthenticatedRequest(
            ActionType.valueOf("USER_RATE_SELLER"), payload, response -> {
                if (response.isSuccess()) {
                    if (onResult != null) onResult.accept(true);
                } else {
                    if (onFailure != null) onFailure.accept(response.getMessage());
                }
            });
    }
}
