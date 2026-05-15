package vn.edu.vnu.uet.group8.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO server push khi phiên đấu giá kết thúc.
 * Gửi qua broadcast EventType.AUCTION_ENDED.
 */
public final class AuctionEndedDTO {
    private int itemId;
    private boolean sold; // true = có người thắng, false = không có ai bid
    private String winnerUsername; // null nếu không có người thắng
    private BigDecimal finalPrice; // giá cuối cùng
    private  Instant endedAt; // thời điểm kết thúc
    //No-arg constructor cho Gson
    private AuctionEndedDTO(){}

    public AuctionEndedDTO(int itemId, boolean sold, String winnerUsername, BigDecimal finalPrice, Instant endedAt){
        this.itemId = itemId;
        this.sold = sold;
        this.winnerUsername = winnerUsername;
        this.finalPrice = finalPrice;
        this.endedAt = endedAt;
    }
    // Getters
    public int getItemId() { return itemId; }
    public boolean isSold() { return sold; }
    public String getWinnerUsername() { return winnerUsername; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public Instant getEndedAt() { return endedAt; }
}
