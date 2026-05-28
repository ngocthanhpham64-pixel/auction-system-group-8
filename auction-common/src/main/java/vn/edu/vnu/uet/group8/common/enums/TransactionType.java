package vn.edu.vnu.uet.group8.common.enums;

public enum TransactionType {
  DEPOSIT     ("Nạp tiền vào ví"),
  WITHDRAW    ("Rút tiền khỏi ví"),
  BID_PAYMENT ("Thanh toán đấu giá"),
  REFUND      ("Hoàn tiền đấu giá"),
  BID_HOLD    ("Tạm giữ khi đặt giá"),
  BID_REFUND  ("Hoàn tiền khi bị vượt"),
  BID_WIN     ("Trừ tiền khi thắng"),
  SALE        ("nhận tiền khi bán được");

  private final String description;

  TransactionType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
