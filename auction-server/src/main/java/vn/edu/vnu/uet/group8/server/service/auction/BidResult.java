package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kết quả sau khi {@link BidProcessor#process} thành công.
 *
 * <p>Immutable — {@link AuctionService} dùng để build broadcast
 * và response về Client mà không cần query DB thêm lần nào.
 */
public final class BidResult {

  private final long transactionId;
  private final int itemId;
  private final int auctionSessionId;
  private final int bidderId;
  private final String bidderUsername;
  private final BigDecimal newPrice;
  private final int totalBids;
  private final Instant currentEndTime;

  /** ID của bidder bị vượt qua -- null nếu đây là bid đầu tiên. */
  private final Integer prevBidderId;

  public BidResult(
      long transactionId,
      int itemId,
      int auctionSessionId,
      int bidderId,
      String bidderUsername,
      BigDecimal newPrice,
      int totalBids,
      Instant currentEndTime,
      Integer prevBidderId) {
    this.transactionId = transactionId;
    this.itemId = itemId;
    this.auctionSessionId = auctionSessionId;
    this.bidderId = bidderId;
    this.bidderUsername = bidderUsername;
    this.newPrice = newPrice;
    this.totalBids = totalBids;
    this.currentEndTime = currentEndTime;
    this.prevBidderId = prevBidderId;
  }

  public long getTransactionId()      { return transactionId; }
  public int getItemId()              { return itemId; }
  public int getAuctionSessionId()    { return auctionSessionId; }
  public int getBidderId()            { return bidderId; }
  public String getBidderUsername()   { return bidderUsername; }
  public BigDecimal getNewPrice()     { return newPrice; }
  public int getTotalBids()           { return totalBids; }
  public Instant getCurrentEndTime()  { return currentEndTime; }

  /** Trả {@code true} nếu có bidder cũ bị vượt qua -- cần hoàn tiền. */
  public boolean hasPreviousBidder()  { return prevBidderId != null; }
  public Integer getPrevBidderId()    { return prevBidderId; }
}