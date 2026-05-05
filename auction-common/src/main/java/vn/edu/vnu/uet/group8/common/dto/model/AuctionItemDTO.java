package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.enums.SpecKey;

/**
 * DTO vận chuyển thông tin Item từ Server → Client qua Socket.
 *
 * Nguyên tắc thiết kế:
 *   - KHÔNG implements Serializable — dùng GSON convert JSON
 *   - KHÔNG chứa sellerId nội bộ — dùng sellerUsername để hiển thị UI
 *   - KHÔNG có logic nghiệp vụ — chỉ chứa data và getter
 *   - id dùng int — nhất quán với INT AUTO_INCREMENT của DB
 *
 * Hai cách tạo AuctionItemDTO:
 *   1. Server gửi về Client  → AuctionItemDTO.from(item, sellerUsername)
 *   2. Broadcast realtime    → AuctionItemDTO.fromBroadcast(item, sellerUsername)
 */
public class AuctionItemDTO {

    // ── Định danh ────────────────────────────────────────
    private int    itemId;        // int, không phải String

    // ── Thông tin hiển thị ───────────────────────────────
    private String        title;
    private String        description;
    private ItemCategory  category;
    private ItemStatus    status;
    private ItemCondition condition;  // NEW / USED / REFURBISHED

    // ── Giá ──────────────────────────────────────────────
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private int        bidCount;      // đếm số lượt bid, tránh JOIN bảng

    // ── Thời gian ─────────────────────────────────────────
    private Instant endTime;

    // ── Người bán ─────────────────────────────────────────
    // sellerUsername thay vì sellerId — Client chỉ cần hiển thị tên,
    // không cần biết ID nội bộ của server
    private String sellerUsername;

    // ── Specs linh hoạt (Hybrid) ──────────────────────────
    // Map<SpecKey.name(), value> — đã parse từ JSON column
    // Nullable: category OTHER có thể không có specs
    private Map<String, String> specs;

    // Constructor private — chỉ tạo qua from() hoặc Reconstructor
    private AuctionItemDTO() {}

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
    public static AuctionItemDTO from(Item item,
                                String sellerUsername,
                                int bidCount) {
      AuctionItemDTO dto = new AuctionItemDTO();
      dto.itemId          = item.getId();
      dto.title           = item.getTitle();
      dto.description     = item.getDescription();
      dto.category        = item.getCategory();
      dto.status          = item.getStatus();
      dto.condition       = item.getCondition();
      dto.startingPrice   = item.getStartingPrice();
      dto.currentPrice    = item.getCurrentPrice();
      dto.bidCount        = bidCount;
      dto.endTime         = item.getEndTime();
      dto.sellerUsername  = sellerUsername;

      // getRawSpecs() trả unmodifiableMap → copy ra để GSON serialize
      dto.specs = item.getSpecs().isEmpty()
                  ? null
                  : Map.copyOf(item.getSpecs());
      return dto;
    }

    /**
     * Overload tiện lợi khi không cần bidCount (hiển thị danh sách nhanh).
     * bidCount = 0 là giá trị mặc định.
     */
    public static AuctionItemDTO from(Item item, String sellerUsername) {
      return from(item, sellerUsername, 0);
    }

    // ════════════════════════════════════════════════════
    // GETTERS
    // Không có setter — DTO là immutable sau khi tạo ra
    // ════════════════════════════════════════════════════

    public int            getItemId()         { return itemId; }
    public String         getTitle()          { return title; }
    public String         getDescription()    { return description; }
    public ItemCategory   getCategory()       { return category; }
    public ItemStatus     getStatus()         { return status; }
    public ItemCondition  getCondition()      { return condition; }
    public BigDecimal     getStartingPrice()  { return startingPrice; }
    public BigDecimal     getCurrentPrice()   { return currentPrice; }
    public int            getBidCount()       { return bidCount; }
    public Instant        getEndTime()        { return endTime; }
    public String         getSellerUsername() { return sellerUsername; }

    /**
     * Trả về toàn bộ specs map.
     * Có thể null nếu item thuộc category OTHER và không có specs.
     * Luôn kiểm tra null trước khi dùng.
     */
    public Map<String, String> getSpecs() {
      return specs != null
              ? Collections.unmodifiableMap(specs)
              : Collections.emptyMap();
    }

    // ── Spec helpers — Client dùng để render form ────────

    /**
     * Đọc một spec theo SpecKey — trả chuỗi rỗng nếu không có.
     * Client dùng để hiển thị từng dòng specs trên UI.
     *
     * Ví dụ:
     *   dto.getSpec(SpecKey.BRAND)     → "Apple"
     *   dto.getSpec(SpecKey.WARRANTY)  → "12 tháng"
     */
    public String getSpec(SpecKey key) {
      if (specs == null || key == null) return "";
      return specs.getOrDefault(key.name(), "");
    }

    /** Kiểm tra spec có tồn tại và không rỗng không */
    public boolean hasSpec(SpecKey key) {
      if (specs == null || key == null) return false;
      String val = specs.get(key.name());
      return val != null && !val.isBlank();
    }

    // ── Helper queries — Client dùng cho UI logic ────────

    /** Item còn đang mở không — dùng để enable/disable nút Bid */
    public boolean isActive() {
      return status == ItemStatus.ACTIVE;
    }

    /** Item đã hết giờ chưa — dùng để hiển thị countdown */
    public boolean isExpired() {
      return endTime != null && Instant.now().isAfter(endTime);
    }

    /** Giá đã tăng so với khởi điểm chưa */
    public boolean hasBids() {
      return bidCount > 0;
    }

    @Override
    public String toString() {
      return "AuctionItemDTO{" +
              "itemId="     + itemId          +
              ", title='"   + title           + '\'' +
              ", category=" + category        +
              ", status="   + status          +
              ", price="    + currentPrice    +
              ", endTime="  + endTime         +
              '}';
    }
}