package vn.edu.vnu.uet.group8.server.service.auction.event;

import java.math.BigDecimal;

/**
 * Sự kiện khi Admin hủy phiên đấu giá.
 * Subscriber cần hoàn tiền cho {@code prevBidderId} nếu không null.
 */
public final class AuctionCancelledEvent {

  private final int        itemId;
  private final int        sessionId;

  // null nếu chưa có bid nào khi bị hủy
  private final Integer    prevBidderId;
  private final BigDecimal refundAmount;

  public AuctionCancelledEvent(
      int itemId, int sessionId,
      Integer prevBidderId, BigDecimal refundAmount) {
    this.itemId        = itemId;
    this.sessionId     = sessionId;
    this.prevBidderId  = prevBidderId;
    this.refundAmount  = refundAmount;
  }

  public int        getItemId()       { return itemId; }
  public int        getSessionId()    { return sessionId; }
  public Integer    getPrevBidderId() { return prevBidderId; }
  public BigDecimal getRefundAmount() { return refundAmount; }
  public boolean    needsRefund()     { return prevBidderId != null; }
}