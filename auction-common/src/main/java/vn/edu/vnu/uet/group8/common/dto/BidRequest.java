package vn.edu.vnu.uet.group8.common.dto;

import java.math.BigDecimal;
public final class BidRequest {
    private final int itemId;
    private final int bidderId;
    private final BigDecimal amount;

    public BidRequest(int itemId,int bidderId,BigDecimal amount){
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.amount = amount;
    }
    // getter
    public int getItemId(){return itemId;}
    public int getBidderId(){return bidderId;}
    public BigDecimal getAmount(){return amount;}
}
