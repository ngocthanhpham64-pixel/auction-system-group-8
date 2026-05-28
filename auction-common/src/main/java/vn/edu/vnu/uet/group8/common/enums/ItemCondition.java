package vn.edu.vnu.uet.group8.common.enums;

/**
 * Trạng thái hàng hóa 
 */
public enum ItemCondition {
  NEW("Mới 100%"),
  LIKENEW("Như mới (99%)"),
  USED("Đã qua sử dụng"), // "Tot (90%)", "Kha (70%)", "Cu (50%)" sẽ map về đây
  USED_AS_IS("Đã qua sử dụng, không bảo hành"),
  REFURBISHED("Đã qua sửa chữa"),
  DAMAGE("Hỏng");

  private final String label;

  ItemCondition(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static ItemCondition fromLabel(String label) {
    for (ItemCondition condition : values()) {
      if (condition.getLabel().equalsIgnoreCase(label)) {
        return condition;
      }
    }
    // Special handling for UI labels that map to 'USED'
    if (label.equals("Tot (90%)") || label.equals("Kha (70%)") || label.equals("Cu (50%)")) {
      return USED;
    }
    return USED; // Default to USED if not found
  }
}
