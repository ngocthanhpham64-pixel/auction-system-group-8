package vn.edu.vnu.uet.group8.common.enums;

/**
 * Định nghĩa các phương thức thanh toán được hỗ trợ trong hệ thống đấu giá.
 */
public enum PaymentMethod {
  
  COD("CASH_ON_DELIVERY", "Thanh toán tiền mặt khi nhận hàng"),
  BANK_TRANSFER("BANK_TRANSFER", "Chuyển khoản ngân hàng (Napas/VietQR)"),
  E_WALLET("E_WALLET", "Ví điện tử (MoMo, ZaloPay, VNPay)"),
  CREDIT_CARD("CREDIT_CARD", "Thẻ tín dụng/Thẻ ghi nợ (Visa/Mastercard)"),
  SYSTEM_BALANCE("SYSTEM_BALANCE", "Thanh toán bằng số dư trong hệ thống");

  private final String code;
  private final String description;

  PaymentMethod(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Chuyển đổi an toàn từ chuỗi String (lấy từ Database hoặc API) sang Enum.
   * 
   * @param code Mã chuỗi của phương thức thanh toán
   * @return PaymentMethod tương ứng
   * @throws IllegalArgumentException Nếu mã không tồn tại trong hệ thống
   */
  public static PaymentMethod fromCode(String code) {
    for (PaymentMethod method : PaymentMethod.values()) {
      if (method.getCode().equalsIgnoreCase(code)) {
        return method;
      }
    }
    throw new IllegalArgumentException(
        "Lỗi hệ thống: Phương thức thanh toán không hợp lệ - " + code);
  }
}