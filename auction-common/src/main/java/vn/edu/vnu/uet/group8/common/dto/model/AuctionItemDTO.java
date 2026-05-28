package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import vn.edu.vnu.uet.group8.common.entity.AuctionSession;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SessionStatus;

/**
 * DTO vận chuyển thông tin Item từ Server → Client qua Socket.
 *
 * Hai cách tạo AuctionItemDTO:
 * 1. Server gửi về Client → AuctionItemDTO.from(item, sellerUsername)
 * 2. Broadcast realtime → AuctionItemDTO.fromBroadcast(item, sellerUsername)
 */
public class AuctionItemDTO {

  // ── Định danh ────────────────────────────────────────
  private int itemId; // int, không phải String
  private Integer sessionId; // ID của phiên đấu giá (có thể null nếu chưa lên sàn)

  // ── Thông tin hiển thị ───────────────────────────────
  private String title;
  private String description;
  private ItemCategory category;
  private ItemCondition condition; // NEW / USED / REFURBISHED
  private SessionStatus status;
  private Instant endTime;
  private BigDecimal currentPrice;
  private int bidCount;
  private Instant createdAt;

  // ── Người bán ─────────────────────────────────────────
  private int sellerId;
  private String sellerUsername;
  private BigDecimal sellerRating;
  private int totalItemsSold;

  // ── Specs linh hoạt (Hybrid) ──────────────────────────
  // Map<SpecKey.name(), value> — đã parse từ JSON column
  // Nullable: category OTHER có thể không có specs
  // private Map<String, String> specs;
  private List<String> imageUrls;

  // Constructor private — chỉ tạo qua from() hoặc Reconstructor
  private AuctionItemDTO() {
  }

  // ════════════════════════════════════════════════════
  // STATIC FACTORY METHODS
  // ════════════════════════════════════════════════════

  /**
   * Chuyển Item entity → AuctionItemDTO để gửi về Client.
   * Server gọi method này trong AuctionService trước khi serialize GSON.
   *
   * @param item           Entity lấy từ DB
   * @param sellerUsername Tên người bán — Service tự query từ UserDAO
   * @param bidCount       Số lượt bid hiện tại
   */
  public static AuctionItemDTO from(AuctionSession ac, Item item,
      int sellerId, String sellerUsername,
      int bidCount) {
    AuctionItemDTO dto = new AuctionItemDTO();
    dto.itemId = item.getId();
    dto.title = item.getTitle();
    dto.description = item.getDescription();
    dto.category = item.getCategory();
    dto.condition = item.getCondition();

    if (ac != null) {
      dto.sessionId = ac.getId();
      dto.status = ac.getStatus();
      dto.endTime = ac.getEndTime();
      dto.currentPrice = ac.getCurrentPrice();
    } else {
      dto.status = SessionStatus.UPCOMING;
    }

    dto.sellerId = sellerId;
    dto.sellerUsername = sellerUsername;
    dto.bidCount = bidCount;
    dto.createdAt = item.getCreatedAt();
    // dto.specs = item.getSpecs().isEmpty() ? null : Map.copyOf(item.getSpecs());
    dto.imageUrls = item.getImageUrls().isEmpty() ? null : List.copyOf(item.getImageUrls());
    return dto;
  }

  public static AuctionItemDTO from(AuctionSession ac, Item item,
      int sellerId, String sellerUsername,
      int bidCount, BigDecimal sellerRating, int totalItemsSold) {
    AuctionItemDTO dto = from(ac, item, sellerId, sellerUsername, bidCount);
    dto.sellerRating = sellerRating;
    dto.totalItemsSold = totalItemsSold;
    return dto;
  }

  public static AuctionItemDTO from(AuctionSession as, Item item, int sellerId, String sellerUsername) {
    return from(as, item, sellerId, sellerUsername, 0);
  }

  public static AuctionItemDTO of(int itemId, String title, String description,
      ItemCategory category, ItemCondition condition,
      SessionStatus status, BigDecimal currentPrice,
      Instant endTime, int sellerId, String sellerUsername,
      Map<String, String> specs, List<String> imageUrls,
      int bidCount, Instant createdAt) {
    AuctionItemDTO dto = new AuctionItemDTO();
    dto.itemId = itemId;
    dto.title = title;
    dto.description = description;
    dto.category = category;
    dto.condition = condition;
    dto.status = status;
    dto.currentPrice = currentPrice;
    dto.endTime = endTime;
    dto.sellerId = sellerId;
    dto.sellerUsername = sellerUsername;
    // dto.specs = specs;
    dto.imageUrls = imageUrls;
    dto.bidCount = bidCount;
    dto.createdAt = createdAt;
    return dto;
  }

  // ════════════════════════════════════════════════════
  // GETTERS
  // Không có setter — DTO là immutable sau khi tạo ra
  // ════════════════════════════════════════════════════

  public int getItemId() {
    return itemId;
  }

  public Integer getSessionId() {
    return sessionId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public ItemCategory getCategory() {
    return category;
  }

  public ItemCondition getCondition() {
    return condition;
  }

  public int getSellerId() {
    return sellerId;
  }

  public String getSellerUsername() {
    return sellerUsername;
  }

  public SessionStatus getStatus() {
    return status;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public BigDecimal getCurrentPrice() {
    return currentPrice;
  }

  public int getBidCount() {
    return bidCount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public BigDecimal getSellerRating() {
    return sellerRating;
  }

  public int getTotalItemsSold() {
    return totalItemsSold;
  }

  public void setCurrentPrice(BigDecimal currentPrice) {
    this.currentPrice = currentPrice;
  }

  public void setSessionId(Integer sessionId) {
    this.sessionId = sessionId;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
  }

  public List<String> getImageUrls() {
    return imageUrls != null
        ? Collections.unmodifiableList(imageUrls)
        : Collections.emptyList();
  }

  /**
   * Trả về toàn bộ specs map.
   * Có thể null nếu item thuộc category OTHER và không có specs.
   * Luôn kiểm tra null trước khi dùng.
   */
  // public Map<String, String> getSpecs() {
  // return specs != null
  // ? Collections.unmodifiableMap(specs)
  // : Collections.emptyMap();
  // }

  // ── Spec helpers — Client dùng để render form ────────

  /**
   * Đọc một spec theo SpecKey — trả chuỗi rỗng nếu không có.
   * Client dùng để hiển thị từng dòng specs trên UI.
   *
   * Ví dụ:
   * dto.getSpec(SpecKey.BRAND) → "Apple"
   * dto.getSpec(SpecKey.WARRANTY) → "12 tháng"
   */
  // public String getSpec(SpecKey key) {
  // if (specs == null || key == null) return "";
  // return specs.getOrDefault(key.name(), "");
  // }

  /** Kiểm tra spec có tồn tại và không rỗng không */
  // public boolean hasSpec(SpecKey key) {
  // if (specs == null || key == null) return false;
  // String val = specs.get(key.name());
  // return val != null && !val.isBlank();
  // }

  // ── Helper queries — Client dùng cho UI logic ────────

  /** Item còn đang mở không — dùng để enable/disable nút Bid */
  public boolean isActive() {
    return status == SessionStatus.ACTIVE;
  }

  /** Item đã hết giờ chưa — dùng để hiển thị countdown */
  public boolean isExpired() {
    return endTime != null && Instant.now().isAfter(endTime);
  }

  // /** Giá đã tăng so với khởi điểm chưa */
  // public boolean hasBids() {
  // return bidCount > 0;
  // }

  @Override
  public String toString() {
    return "AuctionItemDTO{" +
        "itemId=" + itemId +
        ", title='" + title + '\'' +
        ", category=" + category +
        '}';
  }
}