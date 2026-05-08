package vn.edu.vnu.uet.group8.common.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Entity Item - biểu diễn sản phẩm đấu giá.
 * <p>
 * Quy tắc:
 * <ul>
 *   <li>Dùng Builder để tạo mới (có validate dữ liệu đầu vào).</li>
 *   <li>Dùng Reconstructor để tái tạo từ DB (không validate).</li>
 *   <li>Không dùng double, LocalDateTime; thay bằng BigDecimal và Instant.</li>
 *   <li>Soft delete qua trường deleted.</li>
 *   <li>Không implements Serializable; dùng GSON để chuyển JSON.</li>
 * </ul>
 *
 * @author Auction Team
 * @version 2.0
 */
public class Item {

  // ======================== TRƯỜNG DỮ LIỆU ========================
  private int id;                     // 0 = chưa persisted, >0 = đã lưu DB
  private String name;
  private String description;
  private String category;            // Đồng hồ, Trang sức, Xe cổ, ...
  private String status;              // OPEN, CLOSED, UPCOMING
  private BigDecimal startPrice;
  private BigDecimal currentPrice;
  private BigDecimal minBidStep;      // Bước giá tối thiểu
  private Instant endTime;
  private int sellerId;
  private String imageUrl;            // Đường dẫn ảnh hoặc icon code
  private boolean verified;           // Đã qua kiểm định
  private String certBody;            // Tổ chức kiểm định
  private Map<String, String> specs;  // JSON specs (brand, model, warranty, ...)
  private boolean deleted;            // Soft delete flag
  private Instant createdAt;
  private Instant updatedAt;

  // ======================== CONSTRUCTOR PRIVATE ========================
  private Item() {
    this.specs = new HashMap<>();
  }

  // ======================== BUILDER ========================
  /**
   * Khởi tạo Builder cho một Item mới (chưa có trong DB).
   * Validate tất cả tham số bắt buộc.
   *
   * @param name      tên sản phẩm (không null, không rỗng)
   * @param category  danh mục (không null, không rỗng)
   * @param startPrice giá khởi điểm (>=0)
   * @param sellerId  ID người bán (>0)
   * @param endTime   thời gian kết thúc (phải trong tương lai)
   * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
   */
  public static Builder builder(String name, String category, BigDecimal startPrice,
                                int sellerId, Instant endTime) {
    return new Builder(name, category, startPrice, sellerId, endTime);
  }

  public static final class Builder {
    private final Item item = new Item();

    private Builder(String name, String category, BigDecimal startPrice,
                    int sellerId, Instant endTime) {
      if (name == null || name.trim().isEmpty())
        throw new IllegalArgumentException("Tên sản phẩm không được trống");
      if (category == null || category.trim().isEmpty())
        throw new IllegalArgumentException("Danh mục không được trống");
      if (startPrice == null || startPrice.compareTo(BigDecimal.ZERO) < 0)
        throw new IllegalArgumentException("Giá khởi điểm phải >= 0");
      if (sellerId <= 0)
        throw new IllegalArgumentException("Seller ID không hợp lệ");
      if (endTime == null || endTime.isBefore(Instant.now()))
        throw new IllegalArgumentException("Thời gian kết thúc phải trong tương lai");

      item.name = name.trim();
      item.category = category.trim();
      item.startPrice = startPrice;
      item.currentPrice = startPrice;   // ban đầu bằng giá khởi điểm
      item.minBidStep = new BigDecimal("1000000"); // mặc định 1 triệu
      item.sellerId = sellerId;
      item.endTime = endTime;
      item.status = "OPEN";
      item.deleted = false;
      item.createdAt = Instant.now();
      item.updatedAt = Instant.now();
    }

    public Builder description(String description) {
      item.description = description != null ? description.trim() : "";
      return this;
    }

    public Builder minBidStep(BigDecimal step) {
      if (step != null && step.compareTo(BigDecimal.ZERO) > 0)
        item.minBidStep = step;
      return this;
    }

    public Builder imageUrl(String imageUrl) {
      item.imageUrl = imageUrl;
      return this;
    }

    public Builder verified(boolean verified) {
      item.verified = verified;
      return this;
    }

    public Builder certBody(String certBody) {
      item.certBody = certBody;
      return this;
    }

    public Builder putSpec(String key, String value) {
      if (key != null && value != null)
        item.specs.put(key, value);
      return this;
    }

    public Builder specs(Map<String, String> specs) {
      if (specs != null)
        item.specs.putAll(specs);
      return this;
    }

    public Item build() {
      return item;
    }
  }

  // ======================== RECONSTRUCTOR ========================
  /**
   * Khởi tạo Reconstructor cho việc tái tạo Item từ DB.
   * Không validate dữ liệu (dữ liệu từ DB đã an toàn).
   */
  public static Reconstructor reconstruct() {
    return new Reconstructor();
  }

  public static final class Reconstructor {
    private final Item item = new Item();

    public Reconstructor id(int id) {
      item.id = id;
      return this;
    }

    public Reconstructor name(String name) {
      item.name = name;
      return this;
    }

    public Reconstructor description(String description) {
      item.description = description;
      return this;
    }

    public Reconstructor category(String category) {
      item.category = category;
      return this;
    }

    public Reconstructor status(String status) {
      item.status = status;
      return this;
    }

    public Reconstructor startPrice(BigDecimal startPrice) {
      item.startPrice = startPrice;
      return this;
    }

    public Reconstructor currentPrice(BigDecimal currentPrice) {
      item.currentPrice = currentPrice;
      return this;
    }

    public Reconstructor minBidStep(BigDecimal minBidStep) {
      item.minBidStep = minBidStep;
      return this;
    }

    public Reconstructor endTime(Instant endTime) {
      item.endTime = endTime;
      return this;
    }

    public Reconstructor sellerId(int sellerId) {
      item.sellerId = sellerId;
      return this;
    }

    public Reconstructor imageUrl(String imageUrl) {
      item.imageUrl = imageUrl;
      return this;
    }

    public Reconstructor verified(boolean verified) {
      item.verified = verified;
      return this;
    }

    public Reconstructor certBody(String certBody) {
      item.certBody = certBody;
      return this;
    }

    public Reconstructor specs(Map<String, String> specs) {
      if (specs != null)
        item.specs = new HashMap<>(specs);
      return this;
    }

    public Reconstructor deleted(boolean deleted) {
      item.deleted = deleted;
      return this;
    }

    public Reconstructor createdAt(Instant createdAt) {
      item.createdAt = createdAt;
      return this;
    }

    public Reconstructor updatedAt(Instant updatedAt) {
      item.updatedAt = updatedAt;
      return this;
    }

    public Item build() {
      return item;
    }
  }

  // ======================== GETTERS ========================
  public int getId() { return id; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public String getCategory() { return category; }
  public String getStatus() { return status; }
  public BigDecimal getStartPrice() { return startPrice; }
  public BigDecimal getCurrentPrice() { return currentPrice; }
  public BigDecimal getMinBidStep() { return minBidStep; }
  public Instant getEndTime() { return endTime; }
  public int getSellerId() { return sellerId; }
  public String getImageUrl() { return imageUrl; }
  public boolean isVerified() { return verified; }
  public String getCertBody() { return certBody; }
  public Map<String, String> getSpecs() { return new HashMap<>(specs); } // defensive copy
  public boolean isDeleted() { return deleted; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  // ======================== SETTERS (có kiểm soát) ========================
  /**
   * Gán ID cho item sau khi insert vào database.
   * <p><b>Lưu ý:</b> Phương thức này chỉ nên được gọi bởi DAO (cùng package).
   * Nếu cần public trong kiến trúc hiện tại, hãy đảm bảo chỉ gọi từ server.
   *
   * @param id ID do database sinh ra
   * @throws IllegalStateException nếu ID đã được gán trước đó
   */
  public void assignId(int id) {
    if (this.id != 0)
      throw new IllegalStateException("ID đã được gán trước đó");
    this.id = id;
  }

  /**
   * Kiểm tra xem item đã được lưu vào DB chưa.
   * @return true nếu id > 0
   */
  public boolean isPersisted() {
    return id > 0;
  }

  /**
   * Cập nhật giá hiện tại.
   * <p>Chỉ kiểm tra giá không âm, không kiểm tra bước giá (việc đó thuộc về service).
   *
   * @param newPrice giá mới (>=0)
   * @throws IllegalArgumentException nếu newPrice null hoặc âm
   */
  public void setCurrentPrice(BigDecimal newPrice) {
    if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0)
      throw new IllegalArgumentException("Giá không hợp lệ");
    this.currentPrice = newPrice;
    this.updatedAt = Instant.now();
  }

  /**
   * Cập nhật trạng thái item (OPEN, CLOSED, UPCOMING).
   */
  public void setStatus(String status) {
    if (status == null || status.trim().isEmpty())
      throw new IllegalArgumentException("Trạng thái không hợp lệ");
    this.status = status;
    this.updatedAt = Instant.now();
  }

  /**
   * Soft delete: đánh dấu item đã xóa.
   */
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
    this.updatedAt = Instant.now();
  }

  // ======================== PHƯƠNG THỨC NGHIỆP VỤ NHỎ ========================
  /**
   * Kiểm tra xem phiên đấu giá đã kết thúc chưa (dựa trên thời gian hiện tại).
   */
  public boolean isEnded() {
    return Instant.now().isAfter(endTime);
  }

  /**
   * Kiểm tra mức giá đặt có hợp lệ hay không (>= currentPrice + minBidStep).
   * @param bidAmount số tiền muốn đặt
   * @return true nếu hợp lệ
   */
  public boolean isValidBid(BigDecimal bidAmount) {
    if (bidAmount == null) return false;
    BigDecimal required = currentPrice.add(minBidStep);
    return bidAmount.compareTo(required) >= 0;
  }

  // ======================== EQUALS, HASHCODE, TOSTRING ========================
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Item item = (Item) o;
    // Chỉ so sánh bằng ID nếu cả hai đã được persist
    if (this.id != 0 && item.id != 0)
      return this.id == item.id;
    // Nếu một trong hai chưa có ID, fallback về các trường quan trọng (tránh duplicate trong collection)
    return Objects.equals(name, item.name) &&
            Objects.equals(category, item.category) &&
            Objects.equals(startPrice, item.startPrice) &&
            Objects.equals(endTime, item.endTime);
  }

  @Override
  public int hashCode() {
    if (id != 0) return Objects.hash(id);
    // Nếu chưa có ID, dùng các trường ổn định để hash
    return Objects.hash(name, category, startPrice, endTime);
  }

  @Override
  public String toString() {
    return "Item{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", currentPrice=" + currentPrice +
            ", endTime=" + endTime +
            ", status='" + status + '\'' +
            '}';
  }
}