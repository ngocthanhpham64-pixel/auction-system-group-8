package vn.edu.vnu.uet.group8.common.enums;

import com.google.gson.annotations.SerializedName;

/**
 * Các loại event server push xuống client (broadcast, requestId == null)
 * Dùng chung cho cả AuctionBroadcaster (server) và ResponseDispatcher (client).
 */
public enum EventType {
    /** Có bid mới -> upDate giá real-time */
    @SerializedName("price_update")
    PRICE_UPDATE,

    /** Phiên đấu giá kết thúc (SOLD hoặc NO_BID) */
    @SerializedName("auction_ended")
    AUCTION_ENDED,

    /** Thông báo đẩy cho user (bị outbid, thắng đấu giá...) */
    @SerializedName("notification")
    NOTIFICATION,

    /** Bị kick do đăng nhập từ thiết bị khác */
    @SerializedName("kicked")
    KICKED;
}
