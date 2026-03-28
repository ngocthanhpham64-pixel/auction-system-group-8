package vn.edu.vnu.uet.group8.common.factory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.entity.item.Electronics;


/**
 * Lớp Factory để quản lý việc khởi tạo các đối tượng Item.
 * Giúp tập trung logic kiểm tra dữ liệu và gán giá trị mặc định.
 */
public class ItemFactory {
  private String category;
  private String description;
  private BigDecimal startingPrice;
  private BigDecimal currentPrice;
  private String[] additionalAttributes; // Chuỗi chứa các thuộc tính bổ sung, có thể được phân tách bằng dấu phẩy hoặc định dạng khác tùy theo loại mặt hàng
  
  public Item createItem() {
    if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Starting price must be non-negative.");
    }
    
    // Item item;
    // switch (category.toLowerCase()) {
    //   case "electronics":
    //     if (additionalAttributes.length < 2) {
    //       throw new IllegalArgumentException("Electronics requires brand and warranty period.");
    //     }
    //     else if (startingPrice.compareTo(BigDecimal.ZERO) < 0 || currentPrice.compareTo(BigDecimal.ZERO) < 0) {
    //       throw new IllegalArgumentException("Starting price and current price must be non-negative.");
    //     }
    //     String brand = additionalAttributes[0];
    //     int warrantyPeriod = Integer.parseInt(additionalAttributes[1]);
    //     return new Electronics(null, null, false, name, description, BigDecimal.valueOf(startingPrice), BigDecimal.valueOf(currentPrice), LocalDateTime.parse(auctionEndTime), brand, warrantyPeriod);
    //   // Thêm các case khác cho các loại mặt hàng khác nếu cần
    //   default:
    //     throw new IllegalArgumentException("Unknown category: " + category);
    // }
  }
}
