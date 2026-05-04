package vn.edu.vnu.uet.group8.common.dto;

import java.math.BigDecimal;

/**
 * Yêu cầu đặt giá cho một phiên đấu giá.
 */
public final class BidRequest {
    private  int itemId;
    private  int bidderId;
    private  BigDecimal amount;

    private BidRequest(){};

    public BidRequest(int itemId,int bidderId,BigDecimal amount){
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
