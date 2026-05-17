package vn.edu.vnu.uet.group8.common.enums;

public enum UserRole {
  ADMIN("Quản trị viên"),
  SELLER("Người bán"),
  BIDDER("Người mua");

  private final String label;

  UserRole(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
