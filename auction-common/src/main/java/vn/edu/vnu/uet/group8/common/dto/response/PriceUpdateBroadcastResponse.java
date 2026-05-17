package vn.edu.vnu.uet.group8.common.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Broadcast khi có bid mới -- gửi đến TẤT CẢ client đang kết nối.
 * Nhẹ hơn BidResponse -- chỉ chứa thông tin cần thiết để update UI.
 */
public class PriceUpdateBroadcastResponse {

  private final String type = "PRICE_UPDATE"; // Client dùng để route
  private int        itemId;
  private BigDecimal newPrice;
  private int        totalBids;
  private Instant    newEndTime;       // có thể thay đổi do anti-sniping
  private String     bidderUsername;   // hiển thị "Vừa được đặt bởi X"
  private boolean    isExtended;       // true nếu anti-sniping gia hạn

  private PriceUpdateBroadcastResponse() {}

  public static PriceUpdateBroadcastResponse of(
      int itemId, BigDecimal newPrice, int totalBids,
      Instant newEndTime, String bidderUsername,
      boolean isExtended) {
    PriceUpdateBroadcastResponse b = new PriceUpdateBroadcastResponse();
    b.itemId          = itemId;
    b.newPrice        = newPrice;
    b.totalBids       = totalBids;
    b.newEndTime      = newEndTime;
    b.bidderUsername  = bidderUsername;
    b.isExtended      = isExtended;
    return b;
  }

  public String     getType()           { return type; }
  public int        getItemId()         { return itemId; }
  public BigDecimal getNewPrice()       { return newPrice; }
  public int        getTotalBids()      { return totalBids; }
  public Instant    getNewEndTime()     { return newEndTime; }
  public String     getBidderUsername() { return bidderUsername; }
  public boolean    isExtended()        { return isExtended; }
}