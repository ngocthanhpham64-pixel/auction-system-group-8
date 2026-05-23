package vn.edu.vnu.uet.group8.common.entity;

import java.math.BigDecimal;
import java.time.Instant;

public final class AutoBidConfig extends Entity {
  private final int sessionId;
  private final int userId;
  private BigDecimal maxPrice;
  private boolean isActive;

  private AutoBidConfig(Builder b) {
    super(0, Instant.now(), false);
    this.sessionId = b.sessionId;
    this.userId = b.userId;
    this.maxPrice = b.maxPrice;
    this.isActive = true;
  }

  private AutoBidConfig(Reconstructor r) {
    super(r.id, r.createdAt, r.isDeleted);
    this.sessionId = r.sessionId;
    this.userId = r.userId;
    this.maxPrice = r.maxPrice;
    this.isActive = r.isActive;
  }

  public int getSessionId() { return sessionId; }
  public int getUserId() { return userId; }
  public BigDecimal getMaxPrice() { return maxPrice; }
  public boolean isActive() { return isActive; }

  public static Builder builder() { return new Builder(); }
  public static Reconstructor reconstructor() { return new Reconstructor(); }

  // ════════════════════════════════════════════════════
  // Builder
  // ════════════════════════════════════════════════════
  public static class Builder {
    private int sessionId;
    private int userId;
    private BigDecimal maxPrice;

    public Builder sessionId(int sessionId) { this.sessionId = sessionId; return this; }
    public Builder userId(int userId) { this.userId = userId; return this; }
    public Builder maxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; return this; }

    public AutoBidConfig build() {
      return new AutoBidConfig(this);
    }
  }

  // ════════════════════════════════════════════════════
  // Reconstructor
  // ════════════════════════════════════════════════════
  public static class Reconstructor {
    private int id;
    private Instant createdAt;
    private boolean isDeleted;
    private int sessionId;
    private int userId;
    private BigDecimal maxPrice;
    private boolean isActive;

    public Reconstructor id(int id) { this.id = id; return this; }
    public Reconstructor createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Reconstructor isDeleted(boolean isDeleted) { this.isDeleted = isDeleted; return this; }
    public Reconstructor sessionId(int sessionId) { this.sessionId = sessionId; return this; }
    public Reconstructor userId(int userId) { this.userId = userId; return this; }
    public Reconstructor maxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; return this; }
    public Reconstructor isActive(boolean isActive) { this.isActive = isActive; return this; }

    public AutoBidConfig build() {
      return new AutoBidConfig(this);
    }
  }
}
