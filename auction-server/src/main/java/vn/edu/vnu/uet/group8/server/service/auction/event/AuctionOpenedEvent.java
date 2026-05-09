package vn.edu.vnu.uet.group8.server.service.auction.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Sự kiện khi phiên đấu giá bắt đầu (UPCOMING → ACTIVE). */
public final class AuctionOpenedEvent {

  private final int        itemId;
  private final int        sessionId;
  private final BigDecimal startingPrice;
  private final Instant    endTime;

  public AuctionOpenedEvent(
      int itemId, int sessionId,
      BigDecimal startingPrice, Instant endTime) {
    this.itemId        = itemId;
    this.sessionId     = sessionId;
    this.startingPrice = startingPrice;
    this.endTime       = endTime;
  }

  public int        getItemId()        { return itemId; }
  public int        getSessionId()     { return sessionId; }
  public BigDecimal getStartingPrice() { return startingPrice; }
  public Instant    getEndTime()       { return endTime; }
}