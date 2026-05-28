package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.AuctionSession;

/**
 * Đóng gói kết quả validate để truyền sang {@link BidProcessor}.
 *
 * <p>Tránh load lại entity từ DB lần thứ hai sau khi đã validate.
 * Immutable sau khi tạo — không có setter.
 */
public final class BidContext {

  private final User bidder;
  private final Item item;
  private final BigDecimal bidAmount;
  private final AuctionSession auctionSession;
  private final boolean isTieBreaker;

  public BidContext(User bidder, Item item, AuctionSession auctionSession, BigDecimal bidAmount, boolean isTieBreaker) {
    this.bidder = bidder;
    this.item = item;
    this.bidAmount = bidAmount;
    this.auctionSession = auctionSession;
    this.isTieBreaker = isTieBreaker;
  }

  public User getBidder()        { return bidder; }
  public Item getItem()          { return item; }
  public BigDecimal getBidAmount(){ return bidAmount; }
  public AuctionSession getAuctionSession() { return auctionSession; }
  public boolean isTieBreaker()  { return isTieBreaker; }
}