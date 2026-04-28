package vn.edu.vnu.uet.group8.common.config_mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.SpecKey;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.AREA_M2;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.BATHROOMS;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.BEDROOMS;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.BRAND;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.CERTIFICATE;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.COLOR;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.CONDITION;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.COUNTRY_ORIGIN;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.ENGINE_CC;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.ERA;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.FACING_DIRECTION;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.FLOORS;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.FUEL_TYPE;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.GENDER;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.LAND_TYPE;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.LEGAL_STATUS;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.LICENSE_PLATE_CITY;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.LOCATION_CITY;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.LOCATION_DISTRICT;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.MATERIAL;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.MILEAGE_KM;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.MODEL;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.OS;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.PROVENANCE;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.RAM_GB;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.RARITY;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.SCREEN_SIZE_INCH;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.SIZE;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.STORAGE_GB;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.STYLE;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.TRANSMISSION;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.VEHICLE_MAKE;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.VEHICLE_YEAR;
import static vn.edu.vnu.uet.group8.common.enums.SpecKey.WARRANTY_MONTHS;

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

  private static final Map<ItemCategory, CategorySpec> SPEC_MAP = Map.of( // Dùng Map ở đây là vì Map.of () được tối ưu rất tốt trong việc tìm kiếm và bộ nhớ, tốt hơn cả HashMap

      ItemCategory.ELECTRONICS, new CategorySpec(
          List.of(CONDITION, BRAND, WARRANTY_MONTHS),                                  // Dòng này là cho required
          List.of(MODEL, STORAGE_GB, RAM_GB, SCREEN_SIZE_INCH, COLOR, OS)              // Dòng này là cho optional
      ),
      ItemCategory.VEHICLES, new CategorySpec(
          List.of(VEHICLE_MAKE, VEHICLE_YEAR, MILEAGE_KM, FUEL_TYPE),
          List.of(ENGINE_CC, TRANSMISSION, COLOR, LICENSE_PLATE_CITY)
      ),
      ItemCategory.REAL_ESTATE, new CategorySpec(
          List.of(AREA_M2, LAND_TYPE, LEGAL_STATUS, LOCATION_CITY),
          List.of(FLOORS, BEDROOMS, BATHROOMS, FACING_DIRECTION, LOCATION_DISTRICT)
      ),
      ItemCategory.ANTIQUES, new CategorySpec(
          List.of(CONDITION, ERA, COUNTRY_ORIGIN),
          List.of(PROVENANCE, CERTIFICATE, RARITY, MATERIAL)
      ),
      ItemCategory.FASHION, new CategorySpec(
          List.of(CONDITION, SIZE, GENDER),
          List.of(BRAND, COLOR, MATERIAL, STYLE)
      ),
      ItemCategory.OTHER, new CategorySpec(
          List.of(CONDITION),
          List.of()                   // Không có optional → chỉ có description
      )
  );

  /**
     * Lấy danh sách các đặc tính bắt buộc phải có cho một danh mục hàng hóa.
     * Dùng để kiểm tra (validate) dữ liệu trước khi lưu vào Database.
     *
     * @param cat Danh mục hàng hóa cần tra cứu (ví dụ: ELECTRONICS, VEHICLES).
     * @return Danh sách {@link SpecKey} bắt buộc. Trả về cấu hình mặc định nếu danh mục chưa được định nghĩa.
     */
  public static List<SpecKey> getRequiredSpecs(ItemCategory cat) { // Hàm này để lấy thông tin những gì khai báo trong phần bắt buộc
      return SPEC_MAP.getOrDefault(cat, fallback()).required();    // Ở đây .required() là hàm được tự tạo ra trong file .class bắt nguồn từ CategorySpec
  }

  /**
     * Lấy danh sách các đặc tính bổ sung (không bắt buộc) cho một danh mục hàng hóa.
     * Giúp làm phong phú thêm thông tin món hàng nhưng không gây lỗi nếu thiếu.
     *
     * @param cat Danh mục hàng hóa cần tra cứu.
     * @return Danh sách {@link SpecKey} tùy chọn.
     */
  public static List<SpecKey> getOptionalSpecs(ItemCategory cat) { // Tương tự cái trên nhưng là optional
      return SPEC_MAP.getOrDefault(cat, fallback()).optional();
  }

  /**
     * Tổng hợp toàn bộ các đặc tính (cả bắt buộc và tùy chọn) của một danh mục.
     * Thường được dùng để sinh giao diện nhập liệu động (Dynamic UI) cho Client.
     *
     * @param cat Danh mục hàng hóa cần tra cứu.
     * @return Một {@link ArrayList} chứa tất cả các phím đặc tính khả dụng cho danh mục đó.
     */
  public static List<SpecKey> getAllSpecs(ItemCategory cat) {
      CategorySpec spec = SPEC_MAP.getOrDefault(cat, fallback());
      var all = new ArrayList<>(spec.required()); // Trả về 1 ArrayList chứa tất cả các thông tin đã khai báo trong phần bắt buộc
      all.addAll(spec.optional());                // Thêm những phần không bắt buộc
      return all;
  }

  /**
     * Cơ chế dự phòng khi gặp danh mục hàng hóa chưa được cấu hình cụ thể.
     * Đảm bảo hệ thống không bị crash và luôn có ít nhất thông tin về tình trạng món đồ.
     *
     * @return Đối tượng {@link CategorySpec} mặc định chỉ chứa trường CONDITION.
     */
  private static CategorySpec fallback() {                    // Hàm tránh lỗi, nếu dạng hàng không có thì sẽ chỉ cần Condition và không còn thuộc tính nào nữa
      return new CategorySpec(List.of(CONDITION), List.of());
  }

  /**
     * Một bản ghi dữ liệu (Data Record) chứa cấu hình đặc tính.
     * <p>Java Record tự động tạo các phương thức truy cập dữ liệu như {@code required()} và {@code optional()}.</p>
     *
     * @param required Danh sách các đặc tính tối thiểu phải có.
     * @param optional Danh sách các đặc tính có thể bổ sung thêm.
     */
  private record CategorySpec( // Đây là hàm mới từ java 14 trở đi, hàm dạng record, nó thay thế hết các hàm thường dài dòng mà chỉ cần add, get, set,... những hàm cơ bản
      List<SpecKey> required,
      List<SpecKey> optional
  ) {}
}
