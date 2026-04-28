package vn.edu.vnu.uet.group8.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;
import vn.edu.vnu.uet.group8.common.interfaces.SpecAccessor;

/**
 * Đối tượng vận chuyển dữ liệu (DTO) cho vật phẩm đấu giá.
 * <p>Lớp này được thiết kế để truyền tải thông tin Item từ Server về Client thông qua Socket.
 * Chỉ chứa các trường dữ liệu cần thiết cho việc hiển thị giao diện.</p>
 */
public class ItemDTO implements Serializable, SpecAccessor {

  private String itemId;
  private String title;
  private String description;
  private ItemCategory category;
  private ItemStatus status;
  private BigDecimal currentPrice;
  private BigDecimal startingPrice;
  private String sellerId;
  private Instant endTime;
  private Map<String, String> specs;  // JSON đã được parse

  @Override
  public Map<String, String> getRawSpecs() {
    return this.specs;
  }

  // Chuyển từ Entity → DTO (dùng ở Server trước khi gửi)
  public static ItemDTO from(Item item) {
      ItemDTO dto = new ItemDTO();
      dto.itemId       = item.getId();
      dto.title        = item.getTitle();
      dto.description  = item.getDescription();
      dto.category     = item.getCategory();
      dto.status       = item.getStatus();
      dto.currentPrice = item.getCurrentPrice();
      dto.startingPrice= item.getStartingPrice();
      dto.sellerId     = item.getSellerId();
      dto.endTime      = item.getEndTime();
      dto.specs        = item.getRawSpecs();
      return dto;
  }
}
