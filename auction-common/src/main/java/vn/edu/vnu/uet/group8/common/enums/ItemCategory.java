package vn.edu.vnu.uet.group8.common.enums;

public enum ItemCategory {
  WATCHES("Đồng hồ cao cấp"),
  ELECTRONICS("Điện tử"),
  JEWELRY("Trang sức"),
  ART("Nghệ thuật"),
  VEHICLES("Xe cộ"),
  BOOKS("Sách quý"),
  ANTIQUES("Đồ cổ"),
  FASHION("Thời trang"),
  REAL_ESTATE("Bất động sản"),
  OTHER("Khác");

  private final String label;

  ItemCategory(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static ItemCategory fromLabel(String label) {
    for (ItemCategory category : values()) {
      if (category.getLabel().equalsIgnoreCase(label)) {
        return category;
      }
    }
    return OTHER; // Fallback to OTHER if label not found
  }
}
