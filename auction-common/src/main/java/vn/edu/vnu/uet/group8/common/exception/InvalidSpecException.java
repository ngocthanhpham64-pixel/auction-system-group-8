package vn.edu.vnu.uet.group8.common.exception;

/**
 * Ngoại lệ xảy ra khi dữ liệu đặc tính (Specifications) của món hàng không hợp lệ.
 * Thường được ném ra trong quá trình kiểm tra (Validation) dữ liệu JSON Hybrid.
 *
 * @see entity.CategorySpecConfig
 */
public class InvalidSpecException extends AuctionException {
  /**
     * Khởi tạo ngoại lệ với thông tin cụ thể về lỗi của Specification.
     * * @param specKey Tên của đặc tính bị lỗi (ví dụ: "RAM_GB").
     * @param reason Lý do không hợp lệ (ví dụ: "Thiếu trường bắt buộc", "Sai định dạng số").
     */
  public InvalidSpecException(String specKey, String reason) {
      super("Spec [" + specKey + "] không hợp lệ: " + reason);
    }
}
