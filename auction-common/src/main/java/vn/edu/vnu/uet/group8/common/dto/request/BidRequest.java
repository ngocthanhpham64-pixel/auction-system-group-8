package vn.edu.vnu.uet.group8.common.dto.request;

import java.math.BigDecimal;

/**
 * Yêu cầu đặt giá cho một phiên đấu giá.
 */
public final class BidRequest {
    private final int itemId;
    private final int bidderId;
    private final BigDecimal amount;

    private BidRequest() {
        this.itemId = 0;
        this.bidderId = 0;
        this.amount = null;
    }

    public BidRequest(int itemId,int bidderId,BigDecimal amount){
        if (itemId <= 0 || bidderId <= 0) {
            throw new IllegalArgumentException("itemId và bidderId phải > 0");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền đặt giá phải lớn hơn 0");
        }
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.amount = amount;
    }
    // getter
    public int getItemId(){return itemId;}
    public int getBidderId(){return bidderId;}
    public BigDecimal getAmount(){return amount;}

    @Override
    public String toString(){
        return  "BidRequest{itemId=" + itemId + ", bidderId=" + bidderId + ", amount=" + amount + '}';
    }
}
