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
  private final String     winnerUsername;// null nếu NO_BID

  private AuctionEndedEvent(int itemId, String itemTitle,
      Outcome outcome, BigDecimal finalPrice,
      String winnerUsername) {
    this.itemId         = itemId;
    this.itemTitle      = itemTitle;
    this.outcome        = outcome;
    this.finalPrice     = finalPrice;
    this.winnerUsername = winnerUsername;
  }

  public static AuctionEndedEvent sold(int itemId,
      String itemTitle, BigDecimal finalPrice,
      String winnerUsername) {
    return new AuctionEndedEvent(itemId, itemTitle,
        Outcome.SOLD, finalPrice, winnerUsername);
  }

  public static AuctionEndedEvent noBid(
      int itemId, String itemTitle) {
    return new AuctionEndedEvent(itemId, itemTitle,
        Outcome.NO_BID, null, null);
  }

  public int        getItemId()         { return itemId; }
  public String     getItemTitle()      { return itemTitle; }
  public Outcome    getOutcome()        { return outcome; }
  public BigDecimal getFinalPrice()     { return finalPrice; }
  public String     getWinnerUsername() { return winnerUsername; }
  public boolean    hasSold() {
    return outcome == Outcome.SOLD;
  }
}