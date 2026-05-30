package vn.edu.vnu.uet.group8.server.service.auction.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Sự kiện phát sinh khi một lần đặt giá thành công.
 *
 * <p>Subscriber nhận event này để:
 * <ul>
 *   <li>Network layer: broadcast {@code PRICE_UPDATE} đến Client
 *   <li>(Tương lai) Email service: thông báo người bị vượt qua
 *   <li>(Tương lai) Analytics: ghi log thống kê
 * </ul>
 */
public final class BidPlacedEvent {

  private final int        itemId;
  private final int        bidderId;
  private final String     bidderUsername;
  private final BigDecimal newPrice;
  private final int        totalBids;
  private final Instant    newEndTime;
  private final boolean    isExtended;

  // null nếu đây là bid đầu tiên
  private final Integer prevBidderId;

  public BidPlacedEvent(
      int itemId,
      int bidderId,
      String bidderUsername,
      BigDecimal newPrice,
      int totalBids,
      Instant newEndTime,
      boolean isExtended,
      Integer prevBidderId) {
    this.itemId         = itemId;
    this.bidderId       = bidderId;
    this.bidderUsername = bidderUsername;
    this.newPrice       = newPrice;
    this.totalBids      = totalBids;
    this.newEndTime     = newEndTime;
    this.isExtended     = isExtended;
    this.prevBidderId   = prevBidderId;
  }

  public int        getItemId()         { return itemId; }
  public int        getBidderId()       { return bidderId; }
  public String     getBidderUsername() { return bidderUsername; }
  public BigDecimal getNewPrice()       { return newPrice; }
  public int        getTotalBids()      { return totalBids; }
  public Instant    getNewEndTime()     { return newEndTime; }
  public boolean    isExtended()        { return isExtended; }
  public Integer    getPrevBidderId()   { return prevBidderId; }
  public boolean    hasPrevBidder()     { return prevBidderId != null; }
}