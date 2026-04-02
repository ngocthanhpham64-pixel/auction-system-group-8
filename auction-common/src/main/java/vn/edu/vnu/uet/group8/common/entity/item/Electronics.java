package vn.edu.vnu.uet.group8.common.entity.item;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import vn.edu.vnu.uet.group8.common.entity.Item;

public class Electronics extends Item {
  /**
   * Lớp đại diện cho mặt hàng điện tử trong hệ thống đấu giá.
   * Mở rộng từ lớp Item và thêm các thuộc tính đặc trưng cho điện tử.
   */
  private String brand;
  private int warrantyPeriod; // in months

  public Electronics() {
    super();
  }

  /**
   * Constructor đầy đủ cho Item và Vehical, thường dùng khi nạp dữ liệu từ database.
   *
   * @param id               
   * @param createdAt         
   * @param isDeleted         
   * @param name              
   * @param description       
   * @param startingPrice     
   * @param currentPrice        
   * @param brand             
   * @param warrantyPeriod    
   */
  public Electronics(
      String id, 
      LocalDateTime createdAt, 
      boolean isDeleted, 
      String name, 
      String description, 
      BigDecimal startingPrice, 
      BigDecimal currentPrice, 
      String brand, 
      int warrantyPeriod) {
    super(id, createdAt, isDeleted, name, description, startingPrice, currentPrice);
    this.brand = brand;
    this.warrantyPeriod = warrantyPeriod;
  }

  @Override
  public String getCategory() {
    return "Electronics";
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public int getWarrantyPeriod() {
    return warrantyPeriod;
  }

  public void setWarrantyPeriod(int warrantyPeriod) {
    this.warrantyPeriod = warrantyPeriod;
  }
}
