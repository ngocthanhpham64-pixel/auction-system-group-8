package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO để hiển thị lịch sử đặt giá của một người dùng.
 * Kết hợp thông tin về phiên đấu giá và món hàng.
 */
public class UserBidHistoryDTO {
    private int bidId;
    private int sessionId;
    private int itemId;
    private String itemTitle;
    private BigDecimal bidAmount;
    private BigDecimal currentSessionPrice; // Giá hiện tại của phiên đấu giá
    private Instant bidTime;
    private Instant sessionEndTime;

    // Private constructor
    private UserBidHistoryDTO() {}

    public static UserBidHistoryDTO of(
            int bidId,
            int sessionId,
            int itemId,
            String itemTitle,
            BigDecimal bidAmount,
            BigDecimal currentSessionPrice,
            Instant bidTime,
            Instant sessionEndTime) {
        UserBidHistoryDTO dto = new UserBidHistoryDTO();
        dto.bidId = bidId;
        dto.sessionId = sessionId;
        dto.itemId = itemId;
        dto.itemTitle = itemTitle;
        dto.bidAmount = bidAmount;
        dto.currentSessionPrice = currentSessionPrice;
        dto.bidTime = bidTime;
        dto.sessionEndTime = sessionEndTime;
        return dto;
    }

    // Getters
    public int getBidId() {
        return bidId;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public BigDecimal getCurrentSessionPrice() {
        return currentSessionPrice;
    }

    public Instant getBidTime() {
        return bidTime;
    }

    public Instant getSessionEndTime() {
        return sessionEndTime;
    }
}