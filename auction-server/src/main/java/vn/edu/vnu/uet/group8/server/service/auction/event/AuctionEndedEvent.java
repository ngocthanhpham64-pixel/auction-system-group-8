package vn.edu.vnu.uet.group8.server.service.auction.event;

import java.math.BigDecimal;

/**
 * Sự kiện khi phiên đấu giá kết thúc.
 * Dùng static factory để phân biệt SOLD vs NO_BID.
 */
public final class AuctionEndedEvent {

  public enum Outcome { SOLD, NO_BID }

  private final int        itemId;
  private final String     itemTitle;
  private final Outcome    outcome;
  private final BigDecimal finalPrice;    // null nếu NO_BID
  private final Integer    winnerId;      // null nếu NO_BID
  private final String     winnerUsername;// null nếu NO_BID
  private final Integer    sellerId;      // ID người bán

  private AuctionEndedEvent(int itemId, String itemTitle,
      Outcome outcome, BigDecimal finalPrice,
      Integer winnerId, String winnerUsername, Integer sellerId) {
    this.itemId         = itemId;
    this.itemTitle      = itemTitle;
    this.outcome        = outcome;
    this.finalPrice     = finalPrice;
    this.winnerId       = winnerId;
    this.winnerUsername = winnerUsername;
    this.sellerId       = sellerId;
  }

  public static AuctionEndedEvent sold(int itemId,
      String itemTitle, BigDecimal finalPrice,
      Integer winnerId, String winnerUsername, Integer sellerId) {
    return new AuctionEndedEvent(itemId, itemTitle,
        Outcome.SOLD, finalPrice, winnerId, winnerUsername, sellerId);
  }

  public static AuctionEndedEvent noBid(
      int itemId, String itemTitle, Integer sellerId) {
    return new AuctionEndedEvent(itemId, itemTitle,
        Outcome.NO_BID, null, null, null, sellerId);
  }

  public int        getItemId()         { return itemId; }
  public String     getItemTitle()      { return itemTitle; }
  public Outcome    getOutcome()        { return outcome; }
  public BigDecimal getFinalPrice()     { return finalPrice; }
  public Integer    getWinnerId()       { return winnerId; }
  public String     getWinnerUsername() { return winnerUsername; }
  public Integer    getSellerId()       { return sellerId; }
  public boolean    hasSold() {
    return outcome == Outcome.SOLD;
  }
}