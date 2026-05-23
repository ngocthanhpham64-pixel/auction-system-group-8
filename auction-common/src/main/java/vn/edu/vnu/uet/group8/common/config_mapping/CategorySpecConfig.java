package vn.edu.vnu.uet.group8.common.config_mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.SpecKey;

/**
 * Cấu hình các đặc tính (Specifications) cho từng danh mục hàng hóa trong hệ thống đấu giá.
 * Lớp này đóng vai trò là "Bộ quy tắc" để xác định thông tin nào là bắt buộc hoặc không bắt buộc
 * khi người dùng đăng bán một món hàng.
 *
 * @version 1.0
 */
public class CategorySpecConfig {

  /**
   * Khỏi tạo những giá trị cơ bản của một bảng lưu thông tin của đồ vật
   */

  private static final Map<ItemCategory, Set<SpecKey>> REQUIRED_SPECS;

    static {
        Map<ItemCategory, Set<SpecKey>> map = new HashMap<>();
        map.put(ItemCategory.WATCHES,
                Set.of(SpecKey.BRAND, SpecKey.MODEL, SpecKey.YEAR, SpecKey.MATERIAL, SpecKey.ORIGIN,
                        SpecKey.WARRANTY, SpecKey.DIMENSIONS
                ));
        map.put(ItemCategory.ELECTRONICS,
                Set.of(SpecKey.BRAND, SpecKey.MODEL, SpecKey.YEAR, SpecKey.WARRANTY,
                        SpecKey.WARRANTY_MONTHS, SpecKey.STORAGE_GB, SpecKey.RAM_GB,
                        SpecKey.SCREEN_SIZE_INCH, SpecKey.COLOR, SpecKey.OS
                ));
        map.put(ItemCategory.JEWELRY,
                Set.of(SpecKey.MATERIAL, SpecKey.WEIGHT, SpecKey.ORIGIN));
        map.put(ItemCategory.ART,
                Set.of(SpecKey.ARTIST, SpecKey.YEAR, SpecKey.DIMENSIONS,
                        SpecKey.MATERIAL, SpecKey.ERA, SpecKey.ORIGIN
                ));
        map.put(ItemCategory.VEHICLES,
                Set.of(SpecKey.BRAND, SpecKey.MODEL, SpecKey.YEAR, SpecKey.MILEAGE,
                        SpecKey.WARRANTY, SpecKey.WARRANTY_MONTHS, SpecKey.ENGINE_CC,
                        SpecKey.TRANSMISSION, SpecKey.LICENSE_PLATE_CITY, SpecKey.COLOR
                ));
        map.put(ItemCategory.BOOKS,
                Set.of(SpecKey.AUTHOR, SpecKey.PUBLISHER, 
                  SpecKey.YEAR, SpecKey.MATERIAL, SpecKey.ORIGIN
                ));
        map.put(ItemCategory.ANTIQUES,
                Set.of(SpecKey.PERIOD, SpecKey.ORIGIN, SpecKey.MATERIAL, SpecKey.ERA,
                        SpecKey.PROVENANCE, SpecKey.CERTIFICATE, SpecKey.RARITY
                ));
        map.put(ItemCategory.FASHION,
                Set.of(SpecKey.BRAND, SpecKey.SIZE, SpecKey.MATERIAL, SpecKey.GENDER,
                        SpecKey.COLOR, SpecKey.STYLE
                ));
        REQUIRED_SPECS = Collections.unmodifiableMap(map);
    }

  /**
   * 
   */
  private static final Map<ItemCategory, Set<SpecKey>> OPTIONAL_SPECS;
    static {
        Map<ItemCategory, Set<SpecKey>> map = new HashMap<>();
        map.put(ItemCategory.WATCHES, Set.of(SpecKey.WARRANTY, SpecKey.DIMENSIONS));
        map.put(ItemCategory.ELECTRONICS, Set.of(SpecKey.DIMENSIONS, SpecKey.WEIGHT));
        OPTIONAL_SPECS = Collections.unmodifiableMap(map);
    }

  private CategorySpecConfig() {}
  
  /**
   * Lấy toàn bộ các SpecKey (Bắt buộc + Tùy chọn) để UI render form.
   */
  public static List<SpecKey> getRequiredSpecs(ItemCategory category) {
      if (category == null) return Collections.emptyList();
        return Stream.concat(
                REQUIRED_SPECS.getOrDefault(category, Collections.emptySet()).stream(),
                OPTIONAL_SPECS.getOrDefault(category, Collections.emptySet()).stream()
        ).collect(Collectors.toList());
  }

  /**
   * Trả về danh sách tên các SpecKey (String) để dễ dàng chuyển qua JSON cho UI.
   */
  public static List<String> getSpecFields(ItemCategory category) {
      return getRequiredSpecs(category).stream().map(Enum::name).collect(Collectors.toList());
  }
}
