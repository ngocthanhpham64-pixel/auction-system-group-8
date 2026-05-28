package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO đại diện cho một lượt đặt giá trong lịch sử đấu giá.
 * Được server tạo ra và gửi về client để hiển thị trong:
 *  - BidHistoryController: Bảng lịch sử đấu giá
 *  - AuctionDetailController: Danh sách bid real-time
 */
public final class BidRecord {
    private final int bidId;//Id của lượt bid trong DB
    private final int itemId;//ID item đang được đấu giá
    private final int userId;
    private final String displayName;
    // Thông tin bid
    private final BigDecimal amount;// Số tiền đặt
    private final Instant placedAt;// Thời điểm server ghi nhận bid
    //Phân biệt đặt tay và tự động đặt
    //->UI hiển thị badge "Tự động" cho isAutoBid = true
    private final boolean isAutoBid;
    private final String status; // Trạng thái bid: LEADER, FAILED, OUTBID...
    
    //No-arg constructor cho GSON-dùng reflection để deseriable
    private BidRecord(){
        this.bidId = 0;
        this.itemId = 0;
        this.userId = 0;
        this.displayName = null;
        this.amount = null;
        this.placedAt = null;
        this.isAutoBid = false;
        this.status = "LEADER";
    }
    // Constructor từ Builder
    private BidRecord(Builder b){
        this.bidId       = b.bidId;
        this.itemId      = b.itemId;
        this.userId      = b.userId;
        this.displayName = b.displayName;
        this.amount      = b.amount;
        this.placedAt    = b.placedAt;
        this.isAutoBid   = b.isAutoBid;
        this.status      = b.status;
    }
    public int getBidId(){ return bidId;}
    public int getItemId(){ return itemId;}
    public int getUserId(){ return userId;}
    public String getDisplayName(){ return displayName;}
    public BigDecimal getAmount(){ return amount;}
    public Instant getPlacedAt(){ return placedAt;}
    public boolean isAutoBid(){ return isAutoBid;}
    public String getStatus(){ return status;}
    //Entry point
    public static Builder builder(){ return new Builder();}
    @Override
    public String toString() {
        return "BidRecord{"
                + "bidId=" + bidId
                + ", itemId=" + itemId
                + ", userId=" + userId
                + ", displayName='" + displayName + '\''
                + ", amount=" + amount
                + ", placedAt=" + placedAt
                + ", isAutoBid=" + isAutoBid 
                + ", status='" + status + '\'' + '}';
    }
    // Builder
    public static class Builder{
        private int bidId;
        private int itemId;
        private int userId;
        private String displayName;
        private BigDecimal amount;
        private Instant placedAt;
        private boolean isAutoBid = false;//Mặc định là đặt tay
        private String status = "LEADER";

        private Builder(){}

        public Builder bidId(int v){ bidId = v ; return this;}
        public Builder itemId(int v){ itemId = v; return this;}
        public Builder userId(int v){ userId = v; return this;}
        public Builder displayName(String v){ displayName = v; return this;}
        public Builder amount(BigDecimal v){ amount = v; return this;}
        public Builder placedAt(Instant v){ placedAt = v; return this;}
        public Builder isAutoBid(boolean v){ isAutoBid = v; return this;}
        public Builder status(String v){ status = v; return this;}

        /**
         * Validate trước khi tạo object
         * BidRecord do server tạo nên validate chặt, nếu server tạo sai thì client hiển thị sai
         */
        public BidRecord build(){
            // ID phải hợp lệ
            if(bidId <=0 )
                throw new IllegalArgumentException("buildId phải lớn hơn 0");
            if (itemId <= 0)
                throw new IllegalArgumentException("itemId phải > 0");
            if (userId <= 0)
                throw new IllegalArgumentException("userId phải > 0");
            // Tên hiển thị không được rỗng
            if(displayName == null || displayName.isBlank())
                throw new IllegalArgumentException("displayNaem không được null hoặc rỗng");
            // Số tiền bắt buộc và phải dương
            if(amount == null)
                throw new IllegalArgumentException("amount không được null");
            if(amount.compareTo(BigDecimal.ZERO)<=0)
                throw new IllegalArgumentException("amount phải lớn hơn 0");
            return new BidRecord(this);
        }
    }
}
