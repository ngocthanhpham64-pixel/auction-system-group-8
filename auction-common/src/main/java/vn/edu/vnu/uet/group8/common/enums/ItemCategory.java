package vn.edu.vnu.uet.group8.common.enums;

public enum ItemCategory {
  ELECTRONICS   ("Đồ điện tử"),
  FASHION       ("Thời trang"),
  ANTIQUES      ("Đồ cổ"),
  REAL_ESTATE   ("Bất động sản"),
  VEHICLES      ("Xe cộ"),
  OTHER         ("Khác");

  private final String displayName;

  ItemCategory(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
