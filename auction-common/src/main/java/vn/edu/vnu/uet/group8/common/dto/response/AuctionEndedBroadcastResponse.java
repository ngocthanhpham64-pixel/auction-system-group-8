package vn.edu.vnu.uet.group8.common.dto.response;

import java.math.BigDecimal;

/**
 * Broadcast khi phiên đấu giá kết thúc.
 * Scheduler gửi sau khi AuctionClosingService xử lý xong.
 */
public class AuctionEndedBroadcastResponse {

  private final String type = "AUCTION_ENDED";
  private int        itemId;
  private String     itemTitle;
  private String     finalStatus;     // "SOLD" hoặc "ENDED_NO_BID"
  private BigDecimal finalPrice;      // null nếu ENDED_NO_BID
  private String     winnerUsername;  // null nếu ENDED_NO_BID

  private AuctionEndedBroadcastResponse() {}

  /** Phiên kết thúc có người thắng. */
  public static AuctionEndedBroadcastResponse sold(
      int itemId, String itemTitle,
      BigDecimal finalPrice, String winnerUsername) {
    AuctionEndedBroadcastResponse b = new AuctionEndedBroadcastResponse();
    b.itemId         = itemId;
    b.itemTitle      = itemTitle;
    b.finalStatus    = "SOLD";
    b.finalPrice     = finalPrice;
    b.winnerUsername = winnerUsername;
    return b;
  }

  /** Phiên kết thúc không có bid nào. */
  public static AuctionEndedBroadcastResponse noBid(int itemId, String itemTitle) {
    AuctionEndedBroadcastResponse b = new AuctionEndedBroadcastResponse();
    b.itemId      = itemId;
    b.itemTitle   = itemTitle;
    b.finalStatus = "ENDED_NO_BID";
    b.finalPrice  = null;
    b.winnerUsername = null;
    return b;
  }

  public String     getType()           { return type; }
  public int        getItemId()         { return itemId; }
  public String     getItemTitle()      { return itemTitle; }
  public String     getFinalStatus()    { return finalStatus; }
  public BigDecimal getFinalPrice()     { return finalPrice; }
  public String     getWinnerUsername() { return winnerUsername; }
  public boolean    hasSold() {
    return "SOLD".equals(finalStatus);
  }
}