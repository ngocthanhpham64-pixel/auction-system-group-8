package vn.edu.vnu.uet.group8.common.enums;

/**
 * Trạng thái hàng hóa 
 */
public enum ItemCondition {
  NEW("Hàng mới"),
  LIKE_NEW("Như mới (99%)"),
  USED("Đã qua sử dụng"),
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
}
