package vn.edu.vnu.uet.group8.common.enums;

public enum ItemStatus {
  DRAFT     ("Nháp, chưa bán"),
  LISTED    ("Đang lên sàn"),
  SOLD      ("Đã bán thành công"),
  UNSOLD    ("Không có người mua"),
  ARCHIVED  ("Đã xóa do người bán");

  private final String label;

  ItemStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
