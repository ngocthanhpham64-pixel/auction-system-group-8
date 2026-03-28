package vn.edu.vnu.uet.group8.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Item extends Entity {
  /**
   * Lớp trừu tượng cơ sở cho tất cả các mặt hàng trên hệ thống.
   * Cung cấp các thuộc tính định danh và quản lý mặt hàng trên hệ thống.
   */

  private String name;
  private String description;
  private BigDecimal startingPrice;
  private BigDecimal currentPrice;

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
   */

  public Item(String id, LocalDateTime createdAt, boolean isDeleted, String name, String description, BigDecimal startingPrice, BigDecimal currentPrice) {
    super(id, createdAt, isDeleted);
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.currentPrice = currentPrice;
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

  public abstract String getCategory();
}
