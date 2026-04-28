package vn.edu.vnu.uet.group8.common.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.enums.SpecKey;
import vn.edu.vnu.uet.group8.common.interfaces.SpecAccessor;

/**
   * Lớp trừu tượng cơ sở cho tất cả các mặt hàng trên hệ thống.
   * Cung cấp các thuộc tính định danh và quản lý mặt hàng trên hệ thống.
   */
public class Item extends Entity implements SpecAccessor {

  // ── Immutable sau khi tạo ────────────────────────────
  // Những field này không được phép thay đổi sau khi item được tạo
  private final BigDecimal startingPrice;
  private final ItemCategory category;
  private final int sellerId;
  private final String title;

  // ── Mutable có kiểm soát ─────────────────────────────
  // Những field này thay đổi qua method có validation
  private String description;
  private BigDecimal currentPrice;
  private ItemStatus status;
  private ItemCondition condition;
  private Instant endTime;
  private Map<String, String> specs;

  /**
   * Constructor mặc định cho Item mới.
   */
  private Item(Builder b) {
    super(0, Instant.now(), false);
    this.title         = b.title;
    this.category      = b.category;
    this.startingPrice = b.startingPrice;
    this.currentPrice  = b.startingPrice; // ban đầu = startingPrice
    this.sellerId      = b.sellerId;
    this.status        = ItemStatus.UPCOMING;
    this.description   = b.description != null ? b.description : "";
    this.condition     = b.condition;
    this.endTime       = b.endTime;
    this.specs         = b.specs != null
                          ? new HashMap<>(b.specs)
                          : new HashMap<>();
  }

  /**
   * Constructor đầy đủ cho Item, thường dùng khi nạp dữ liệu từ database.
   *
   * @param id
   * @param createdAt
   * @param isDeleted
   * @param title
   * @param description
   * @param startingPrice
   * @param currentPrice
   * @param status
   * @param condition
   * @param sellerId
   * @param category
   * @param endTime
   * @param specs
   */

  private Item(Reconstructor r) {
    super(r.id, r.createdAt, r.isDeleted);
    this.title          = r.title;
    this.description    = r.description;
    this.startingPrice  = r.startingPrice;
    this.currentPrice   = r.currentPrice;
    this.status         = r.status;
    this.condition      = r.condition;
    this.sellerId       = r.sellerId;
    this.endTime        = r.endTime;
    this.category       = r.category;
    this.specs          = r.specs != null
                          ? new HashMap<>(specs)
                          : new HashMap<>();
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
    private String title;
    private String description;         //nullable
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private ItemStatus status;
    private ItemCondition condition;
    private Integer sellerId;
    private ItemCategory category;
    private Instant endTime;
    private Map<String, String> specs;

    public Reconstructor id(int id) {
      this.id = id; return this;
    }
    public Reconstructor createdAt(Instant v) {
      this.createdAt = v; return this;
    }
    public Reconstructor isDeleted(boolean v) {
      this.isDeleted = v; return this;
    }
    public Reconstructor title(String v) {
      this.title = v; return this;
    }
    public Reconstructor description(String v) {
      this.description = v; return this;
    }
    public Reconstructor startingPrice(BigDecimal v) {
      this.startingPrice = v; return this;
    }
    public Reconstructor currentPrice(BigDecimal v) {
      this.currentPrice = v; return this;
    }
    public Reconstructor status(ItemStatus v) {
      this.status = v; return this;
    }
    public Reconstructor condition(ItemCondition v) {
      this.condition = v; return this;
    }
    public Reconstructor sellerId(int v) {
      this.sellerId = v; return this;
    }
    public Reconstructor category(ItemCategory v) {
      this.category = v; return this;
    }
    public Reconstructor endTime(Instant v) {
      this.endTime = v; return this;
    }
    public Reconstructor specs(Map<String, String> v) {
      this.specs = v; return this;
    }

    public Item build() {
      requireNonNull(id, "id");
      requireNonNull(createdAt, "createdAt");
      requireNonNull(isDeleted, "isDeleted");
      requireNonNull(title, "title");
      requireNonNull(startingPrice, "startingPrice");
      requireNonNull(currentPrice, "currentPrice");
      requireNonNull(status, "status");
      requireNonNull(sellerId, "sellerId");
      requireNonNull(category, "category");
      requireNonNull(specs, "specs");

      return new Item(this);
    }

    public void requireNonNull(Object value, String fieldName) {
      if (value == null)
        throw new IllegalStateException(
          "Reconstructor thiếu field bắt buộc: [" + fieldName + "]. "
          + "Kiểm tra lại ItemDAO.mapRow()");
    }
  }

  // ════════════════════════════════════════════════════
  // BUILDER
  // ════════════════════════════════════════════════════
  public static class Builder {
    //Required
    private final int sellerId;
    private final String title;
    private final ItemCategory category;
    private final BigDecimal startingPrice;
    private Instant endTime;

    //Optional
    private String description        = "";
    private ItemCondition condition   = ItemCondition.USED;
    private Map<String, String> specs = new HashMap();

    /**
     * Những thứ bắt buộc trước
     */
  public Builder(int sellerId, String title, ItemCategory category, BigDecimal startingPrice, Instant endTime) {
      if (sellerId <= 0)
        throw new IllegalArgumentException("sellerId không tồn tại");
      if (title == null || title.isBlank())
        throw new IllegalArgumentException("Tiêu đề item không được trống");
      if (title.length() > 200)
        throw new IllegalArgumentException("Tiêu đề không được vượt quá 200 ký tự");
      if (category == null)
        throw new IllegalArgumentException("Category không được null");
      if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) < 0)
        throw new IllegalArgumentException("Giá khởi điểm không được âm");
      if (endTime == null && endTime.isBefore(Instant.now())) {
        throw new IllegalArgumentException("Thời gian kết thúc phải ở tương lai");
      }
      this.sellerId       = sellerId;
      this.title          = title.trim();
      this.category       = category;
      this.startingPrice  = startingPrice;
      this.endTime        = endTime;
    }

    public Builder description(String description) {
      this.description = description != null ? description.trim() : "";
      return this;
    }

    public Builder condition(ItemCondition condition) {
      this.condition = condition != null ? condition : ItemCondition.USED;
      return this;
    }

    /**
     * Truyền vào toàn bộ specs map một lần.
     * ItemFactory sẽ build map này từ CategorySpecConfig
     * trước khi gọi Builder.
     */
    public Builder specs(Map<String, String> specs) {
      this.specs = specs != null ? new HashMap<>(specs) : new HashMap<>();
      return this;
    }

    /**
     * Thêm từng spec một — tiện khi test hoặc tạo item đơn giản.
     */
    public Builder putSpecs(String key, String value) {
      if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
        this.specs.put(key, value);
      }
      return this;
    }

    public Item build() {
      return new Item(this);
    }
  }

  // ════════════════════════════════════════════════════
  // GETTERS
  // ════════════════════════════════════════════════════
  public Instant getEndTime() {
    return endTime;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getStartingPrice() {
    return startingPrice;
  }

  public BigDecimal getCurrentPrice() {
    return currentPrice;
  }

  public ItemCategory getCategory() {
    return category;
  }

  public ItemStatus getStatus() {
    return status;
  }
  public ItemCondition getCondition() {
    return condition;
  }

  public int getSellerId() {
    return sellerId;
  }

  public Map<String, String> getSpecs() {
    return Collections.unmodifiableMap(specs);
  }

  // ════════════════════════════════════════════════════
  // SPEC HELPERS — interface SpecAccessor
  // ════════════════════════════════════════════════════\

  /**
   * Đọc một spec theo SpecKey enum.
   * Trả về chuỗi rỗng nếu không có — không trả null
   * để tầng UI không phải null-check liên tục.
   */
  // Helper đọc/ghi specs — dùng SpecKey enum thay vì String thô
  public String getSpecs(SpecKey key) {
    return specs.getOrDefault(key.name(), "");
  }

  /** Đọc theo raw String — dùng cho custom spec */
  public String getSpecs(String rawKey) {
    return specs.getOrDefault(rawKey, "");
  }

  public boolean hasSpec(SpecKey key) {
    return specs.containsKey(key.name());
  } 

  public boolean hasSpec(String rawKey) {
    return specs.containsKey(rawKey);
  }

  @Override
  public Map<String, String> getRawSpecs() {
    return Collections.unmodifiableMap(specs);
  }

  // ════════════════════════════════════════════════════
  // SETTERS CÓ KIỂM SOÁT
  // ════════════════════════════════════════════════════

  /**
   * Cập nhật mô tả — người bán có thể sửa trước khi ACTIVE.
   * Sau khi ACTIVE thì không cho sửa nữa (tránh gian lận).
   */
  public void setDescription(String description) {
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Mô tả không được trống");
    }
    if (status == ItemStatus.ACTIVE || status == ItemStatus.SOLD) {
      throw new IllegalStateException("Không thể sửa mô tả khi item đang đấu giá hoặc đã bán");
    }
    this.description = description.trim();
  }

  /**
   * Cập nhật condition — chỉ cho phép trước khi ACTIVE.
   */
  public void setCondition(ItemCondition condition) {
    if (status == ItemStatus.ACTIVE || status == ItemStatus.SOLD) {
      throw new IllegalStateException("Không thể sửa tình trạng khi item đang đấu giá");
    }
    if (condition == null) {
      throw new IllegalArgumentException("Condition không được null");
    }
    this.condition = condition;
  }

  /**
   * Cập nhật giá hiện tại khi có bid mới.
   * Chỉ AuctionService gọi method này — không để UI gọi trực tiếp.
   * Giá mới phải cao hơn giá hiện tại.
   */
  public void raiseCurrentPrice(BigDecimal newPrice) {
    if (status != ItemStatus.ACTIVE) {
      throw new IllegalStateException("Chỉ có thể cập nhật giá khi item đang ACTIVE");
    }
    if (newPrice == null || newPrice.compareTo(currentPrice) <= 0) {
      throw new IllegalStateException("Giá mới (" + newPrice + ") phải cao hơn giá hiện tại (" + currentPrice + ")");
    }
    this.currentPrice = newPrice;
  }

  /**
   * Chuyển trạng thái item — AuctionService kiểm soát luồng.
   *
   * Luồng hợp lệ:
   *   UPCOMING → ACTIVE → SOLD
   *   UPCOMING → ACTIVE → ENDED_NO_BID
   *   UPCOMING → CANCELLED
   *   ACTIVE   → CANCELLED  (admin can thiệp)
   */
  public boolean transitionStatus(ItemStatus from, ItemStatus to) {
    this.status = to;
    return switch (from) {
      case UPCOMING     -> to == ItemStatus.ACTIVE
                        || to == ItemStatus.CANCELLED;
      case ACTIVE       -> to == ItemStatus.SOLD
                        || to == ItemStatus.ENDED_NO_BID
                        || to == ItemStatus.CANCELLED;
      // SOLD, ENDED_NO_BID, CANCELLED là trạng thái cuối
      default           -> false;
    };
  }

  /**
   * Cập nhật hoặc thêm một spec sau khi item đã tạo.
   * Chỉ cho phép khi chưa ACTIVE.
   */
  public void putSpecs(SpecKey key, String value) {
    if (status == ItemStatus.ACTIVE || status == ItemStatus.SOLD) {
      throw new IllegalStateException("Không thể sửa specs khi item đang đấu giá");
    }
    if (key == null) {
      return;
    }
    if (value != null && !value.isBlank()) {
      specs.put(key.name(), value.trim());
    }
  }

  public void putCustomSpec(String rawKey, String value) {
    // Key tự do không qua SpecKey enum
    // Thêm prefix để phân biệt với key chuẩn
    specs.put("custom_" + rawKey, value);
  }

  // ════════════════════════════════════════════════════
  // HELPER QUERIES
  // ════════════════════════════════════════════════════
  public boolean isActive() {
    return status == ItemStatus.ACTIVE && !isDeleted();
  }

  public boolean isExpired() {
    return Instant.now().isAfter(endTime);
  }

  /**
   * Anti-sniping: còn dưới N phút không?
   * AuctionService gọi trước mỗi lần xử lý bid.
   */
  public boolean isInSnipingWindow(int minutes) {
    Instant windows = endTime.minusSeconds((long) minutes * 60);
    return Instant.now().isAfter(windows);
  }

  // ════════════════════════════════════════════════════
  // OVERRIDE
  // ════════════════════════════════════════════════════

  @Override
  public String toString() {
    return "Item{" +
            "id='"           + getId()        + '\'' +
            ", title='"      + title          + '\'' +
            ", category="    + category       +
            ", status="      + status         +
            ", currentPrice=" + currentPrice  +
            ", endTime="     + endTime        +
            ", isDeleted="   + isDeleted()    +
            '}';
  }
}