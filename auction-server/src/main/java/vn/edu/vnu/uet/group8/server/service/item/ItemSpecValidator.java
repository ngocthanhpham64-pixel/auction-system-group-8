package vn.edu.vnu.uet.group8.server.service.item;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.config_mapping.CategorySpecConfig;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.SpecKey;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;

/**
 * Kiểm tra tính hợp lệ của specs theo từng category.
 *
 * <p>
 * Không phụ thuộc vào bất kỳ DAO nào — chỉ dùng
 * {@link CategorySpecConfig}. Có thể tái sử dụng bởi
 * {@code ItemWriteService} và {@code AuctionSessionService}
 * mà không cần import toàn bộ ItemService.
 */
public class ItemSpecValidator {

  private static final Logger logger = LoggerFactory.getLogger(ItemSpecValidator.class);

  /**
   * Kiểm tra specs có đủ required fields theo category không.
   *
   * <p>
   * Chỉ kiểm tra required — optional bỏ qua hoàn toàn.
   * Liệt kê rõ tất cả key còn thiếu trong một lần ném exception
   * thay vì dừng ở key đầu tiên — giúp Client hiển thị
   * đầy đủ lỗi cho người dùng.
   *
   * @param category category của item
   * @param specs    map specs người dùng nhập vào
   * @throws ValidationException nếu thiếu ít nhất một required spec,
   *                             kèm danh sách tên key bị thiếu
   */
  public void validate(ItemCategory category) {

    if (category == null) {
      throw new ValidationException("Category không được null");
    }

    // List<SpecKey> required =
    // CategorySpecConfig.getRequiredSpecs(category);

    // Collect tất cả key còn thiếu — không dừng ở cái đầu tiên
    // List<String> missing = required.stream()
    // .filter(key -> isMissing(specs, key))
    // .map(SpecKey::name)
    // .collect(Collectors.toList());

    // if (!missing.isEmpty()) {
    // logger.debug(
    // "validateSpecs thất bại: category={}, missing={}",
    // category, missing);

    // Không còn throw ValidationException để người dùng có thể để trống specs
    // }

    // logger.debug("validateSpecs hợp lệ: category={}", category);
  }

  // ── Private helpers ─────────────────────────────────

  /**
   * Kiểm tra một SpecKey có bị thiếu trong map không.
   * Thiếu = null, không có key, hoặc giá trị rỗng/chỉ khoảng trắng.
   */
  // private boolean isMissing(Map<String, String> specs, SpecKey key) {
  // if (specs == null) return true;
  // String value = specs.get(key.name());
  // return value == null || value.isBlank();
  // }
}