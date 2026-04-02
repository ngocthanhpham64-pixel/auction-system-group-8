package vn.edu.vnu.uet.group8.common.entity;

import java.math.BigDecimal;
import java.time.Instant;
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
public abstract class Item extends Entity implements SpecAccessor {

  private Map<String, String> specs = new HashMap<>();

  private String name;
  private String description;
  private BigDecimal startingPrice;
  private BigDecimal currentPrice;
  private ItemCategory category;
  private ItemStatus status;
  private ItemCondition condition;
  private String sellerId;
  private Instant endTime;

  /**
   * Constructor mặc định cho Item mới.
   */

  public Item() {
    super();
  }

  /**
   * Constructor đầy đủ cho Item, thường dùng khi nạp dữ liệu từ database.
   *
   * @param id
   * @param createdAt
   * @param isDeleted
   * @param name
   * @param description
   * @param startingPrice
   * @param currentPrice
   * @param status
   * @param condition
   * @param sellerId
   */

  public Item(
      String id, 
      Instant createdAt, 
      boolean isDeleted, 
      String name, 
      String description, 
      BigDecimal startingPrice, 
      BigDecimal currentPrice,
      ItemStatus status, 
      ItemCondition condition, 
      String sellerId,
      Instant endTime) {
    super(id, createdAt, isDeleted);
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.currentPrice = currentPrice;
    this.status = status;
    this.condition = condition;
    this.sellerId = sellerId;
    this.endTime = endTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
  }

  public void putCustomSpec(String rawKey, String value) {
    // Key tự do không qua SpecKey enum
    // Thêm prefix để phân biệt với key chuẩn
    specs.put("custom_" + rawKey, value);
  }

  // Helper đọc/ghi specs — dùng SpecKey enum thay vì String thô
  public String getSpecs(SpecKey key) {
    return specs.getOrDefault(key.name(), "");
  }

  public void putSpecs(SpecKey key, String value) {
    if (value != null && !value.isBlank()) {
      specs.put(key.name(), value);
    }
  }

  public boolean hasSpec(SpecKey key) {
    return specs.containsKey(key.name());
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(BigDecimal startingPrice) {
    this.startingPrice = startingPrice;
  }

  public BigDecimal getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(BigDecimal currentPrice) {
    this.currentPrice = currentPrice;
  }

  public void setCategory(ItemCategory category) {
    this.category = category;
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

  public String getSellerId() {
    return sellerId;
  }
}
