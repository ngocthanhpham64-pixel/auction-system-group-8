package vn.edu.vnu.uet.group8.common.entity;

import java.math.BigDecimal;
import java.time.Instant;

import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

/**
 * Lớp biểu diễn một phiên đấu giá cho một sản phẩm.
 */
public final class AuctionSession extends Entity {

  // ── Immutable sau khi tạo ────────────────────────────
  private final int itemId;
  private final BigDecimal startingPrice;
  private final Instant startTime;
  private final Instant endTime;

  // ── Mutable có kiểm soát ─────────────────────────────
  private BigDecimal currentPrice;
  private SessionStatus status;
  private Integer highestBidderId;
  private int bidCount;

  private AuctionSession(Builder b) {
    super(0, Instant.now(), false);
    this.itemId = b.itemId;
    this.startingPrice = b.startingPrice;
    this.currentPrice = b.startingPrice;
    this.status = SessionStatus.UPCOMING;
    this.startTime = b.startTime;
    this.endTime = b.endTime;
    this.highestBidderId = b.highestBidderId;
    this.bidCount = b.bidCount != null ? b.bidCount : 0;
  }

  private AuctionSession(Reconstructor r) {
    super(r.id, r.createdAt, r.isDeleted);
    this.itemId = r.itemId;
    this.startingPrice = r.startingPrice;
    this.currentPrice = r.currentPrice;
    this.status = r.status;
    this.startTime = r.startTime;
    this.endTime = r.endTime;
    this.highestBidderId = r.highestBidderId;
    this.bidCount = r.bidCount;
  }

  public static Reconstructor reconstructor() {
    return new Reconstructor();
  }

  // ════════════════════════════════════════════════════
  // RECONSTRUCTOR
  // ════════════════════════════════════════════════════
  public static class Reconstructor {
    private Integer id;
    private Instant createdAt;
    private Boolean isDeleted;
    private Integer itemId;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private SessionStatus status;
    private Instant startTime;
    private Instant endTime;
    private Integer highestBidderId;
    private Integer bidCount;

    public Reconstructor id(int id) { this.id = id; return this; }
    public Reconstructor createdAt(Instant v) { this.createdAt = v; return this; }
    public Reconstructor isDeleted(boolean v) { this.isDeleted = v; return this; }
    public Reconstructor itemId(int v) { this.itemId = v; return this; }
    public Reconstructor startingPrice(BigDecimal v) { this.startingPrice = v; return this; }
    public Reconstructor currentPrice(BigDecimal v) { this.currentPrice = v; return this; }
    public Reconstructor status(SessionStatus v) { this.status = v; return this; }
    public Reconstructor startTime(Instant v) { this.startTime = v; return this; }
    public Reconstructor endTime(Instant v) { this.endTime = v; return this; }
    public Reconstructor highestBidderId(Integer v) { this.highestBidderId = v; return this; }
    public Reconstructor bidCount(int v) { this.bidCount = v; return this; }

    public AuctionSession build() {
      requireNonNull(id, "id");
      requireNonNull(createdAt, "createdAt");
      requireNonNull(isDeleted, "isDeleted");
      requireNonNull(itemId, "itemId");
      requireNonNull(startingPrice, "startingPrice");
      requireNonNull(currentPrice, "currentPrice");
      requireNonNull(status, "status");
      requireNonNull(startTime, "startTime");
      requireNonNull(endTime, "endTime");
      requireNonNull(bidCount, "bidCount");
      return new AuctionSession(this);
    }

    private void requireNonNull(Object value, String fieldName) {
      if (value == null)
        throw new IllegalStateException("Reconstructor thiếu field bắt buộc: [" + fieldName + "].");
    }
  }

  // ════════════════════════════════════════════════════
  // BUILDER
  // ════════════════════════════════════════════════════
  public static class Builder {
    private final int itemId;
    private final BigDecimal startingPrice;
    private final Instant startTime;
    private final Instant endTime;

    private Integer highestBidderId;
    private Integer bidCount;

    public Builder(int itemId, BigDecimal startingPrice, Instant startTime, Instant endTime) {
      if (itemId <= 0) throw new IllegalArgumentException("itemId không hợp lệ");
      if (startingPrice == null || startingPrice.intValue() < 0) throw new IllegalArgumentException("Giá khởi điểm không được âm");
      if (startTime == null) throw new IllegalArgumentException("Thời gian bắt đầu không được để trống");
      if (endTime == null || endTime.isBefore(Instant.now())) throw new IllegalArgumentException("Thời gian kết thúc phải ở tương lai");
      if (endTime.isBefore(startTime)) throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
      
      this.itemId = itemId;
      this.startingPrice = startingPrice;
      this.startTime = startTime;
      this.endTime = endTime;
    }

    public Builder highestBidderId(Integer highestBidderId) {
      this.highestBidderId = highestBidderId;
      return this;
    }

    public Builder bidCount(Integer bidCount) {
      this.bidCount = bidCount == null ? 0 : bidCount;
      return this;
    }

    public AuctionSession build() {
      return new AuctionSession(this);
    }
  }

  // ════════════════════════════════════════════════════
  // GETTERS
  // ════════════════════════════════════════════════════
  public int getItemId() { return itemId; }
  public BigDecimal getStartingPrice() { return startingPrice; }
  public BigDecimal getCurrentPrice() { return currentPrice; }
  public SessionStatus getStatus() { return status; }
  public Instant getStartTime() { return startTime; }
  public Instant getEndTime() { return endTime; }
  public Integer getHighestBidderId() { return highestBidderId; }
  public int getBidCount() { return bidCount; }

  // ════════════════════════════════════════════════════
  // SETTERS CÓ KIỂM SOÁT
  // ════════════════════════════════════════════════════
  public void incrementBidCount() {
    this.bidCount++;
  }

  public void raiseCurrentPrice(BigDecimal newPrice) {
    if (status != SessionStatus.ACTIVE) {
      throw new IllegalStateException("Chỉ có thể cập nhật giá khi session đang ACTIVE");
    }
    if (newPrice == null || newPrice.compareTo(currentPrice) <= 0) {
      throw new IllegalStateException("Giá mới (" + newPrice + ") phải cao hơn giá hiện tại (" + currentPrice + ")");
    }
    this.currentPrice = newPrice;
  }

  public void setHighestBidderId(int bidderId) {
    if (bidderId <= 0) {
      throw new IllegalArgumentException("ID người đặt giá không tồn tại");
    }
    this.highestBidderId = bidderId;
  }

  public boolean transitionStatus(SessionStatus from, SessionStatus to) {
    if (this.status != from) {
      return false;
    }
    boolean isValid = switch (from) {
      case UPCOMING     -> to == SessionStatus.ACTIVE || to == SessionStatus.CANCELLED;
      case ACTIVE       -> to == SessionStatus.SOLD || to == SessionStatus.ENDED_NO_BID || to == SessionStatus.CANCELLED;
      default           -> false;
    };
    if (isValid) {
      this.status = to;
    }
    return isValid;
  }

  // ════════════════════════════════════════════════════
  // HELPER QUERIES
  // ════════════════════════════════════════════════════
  public boolean isActive() {
    return status == SessionStatus.ACTIVE && !isDeleted();
  }

  public boolean isExpired() {
    return Instant.now().isAfter(endTime);
  }

  public boolean isInSnipingWindow(int minutes) {
    Instant window = endTime.minusSeconds((long) minutes * 60);
    return Instant.now().isAfter(window);
  }

  // ════════════════════════════════════════════════════
  // OVERRIDE
  // ════════════════════════════════════════════════════
  @Override
  public String toString() {
    return "AuctionSession{" +
            "id='"           + getId()        + '\'' +
            ", itemId='"     + itemId         + '\'' +
            ", status="      + status         +
            ", currentPrice=" + currentPrice  +
            ", startTime="   + startTime      +
            ", endTime="     + endTime        +
            ", isDeleted="   + isDeleted()    +
            '}';
  }
}