package vn.edu.vnu.uet.group8.common.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * DTO trạng thái phiên đấu giá - gửi từ server xuống client.
 * Immutable: Tất cả fields final, List trả về unmodifiable.
 */

public final class AuctionStatusDTO {
    private final int itemId;
    private final BigDecimal currentPrice;
    private final Instant endTime;
    private final List<BidRecord> bidHistory;

    private AuctionStatusDTO(){
        this.itemId = 0;
        this.currentPrice = null;
        this.endTime = null;
        this.bidHistory = null;
    }

    public AuctionStatusDTO(int itemId,BigDecimal currentPrice,
                            Instant endTime, List<BidRecord> bidHistory){
        if(itemId <= 0)
            throw new IllegalArgumentException("itemId phải > 0");
        if(currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO)<0)
            throw new IllegalArgumentException("currentPrice không hợp lệ");
        if(endTime == null)
            throw new IllegalArgumentException("endTime không được null");
        this.itemId = itemId;
        this.currentPrice = currentPrice;
        this.endTime = endTime;
        this.bidHistory = bidHistory != null ? List.copyOf(bidHistory):List.of();
    }
    //Getters
    public int getItemId(){ return itemId;}
    public BigDecimal getCurrentPrice(){ return currentPrice;}
    public Instant getEndTime(){ return endTime;}
    // Trả về unmodifiable List - không ai xóa/sửa được từ ngoài
    public List<BidRecord> getBidHistory(){
        return Collections.unmodifiableList(bidHistory);
    }
    @Override
    public String toString(){
        return "AuctionStatusDTO{itemId=" + itemId
                + ", currentPrice=" + currentPrice
                + ", endTime=" + endTime
                + ", bidHistorySize=" + (bidHistory != null ? bidHistory.size() : 0) + '}';
    }
}
