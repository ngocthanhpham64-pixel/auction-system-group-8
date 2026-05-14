package vn.edu.vnu.uet.group8.common.enums;

public enum TransactionStatus {
  SUCCESS("Thành công"),
  PENDING("Đang xử lý"),
  FAILED("Thất bại"),
  CANCELLED("Đã hủy");

  private final String label;

  TransactionStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
