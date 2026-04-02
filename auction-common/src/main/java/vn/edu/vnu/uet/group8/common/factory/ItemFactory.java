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
  public static Item createItem(
    String name,
    String category,
    String description,
    BigDecimal startingPrice,
    BigDecimal currentPrice,
    String[] additionalAttributes) {

    if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Starting price must be non-negative.");
    }
    
    Item item;
    switch (category.toLowerCase()) {
      case "electronics":
        if (additionalAttributes.length < 2) {
          throw new IllegalArgumentException("Electronics requires brand and warranty period.");
        }
        else if (startingPrice.compareTo(BigDecimal.ZERO) < 0 || currentPrice.compareTo(BigDecimal.ZERO) < 0) {
          throw new IllegalArgumentException("Starting price and current price must be non-negative.");
        }
        String brand = additionalAttributes[0];
        int warrantyPeriod = Integer.parseInt(additionalAttributes[1]);
        item = new Electronics();
        item.setName(name);
        item.setDescription(description);
        item.setStartingPrice(startingPrice);
        item.setCurrentPrice(startingPrice); // Giá khởi điểm bằng giá hiện tại khi tạo mới
        ((Electronics) item).setBrand(brand);
        ((Electronics) item).setWarrantyPeriod(warrantyPeriod);
        return item;

      case ""
      // Thêm các case khác cho các loại mặt hàng khác nếu cần
      default:
        throw new IllegalArgumentException("Unknown category: " + category);
    }
  }
}
