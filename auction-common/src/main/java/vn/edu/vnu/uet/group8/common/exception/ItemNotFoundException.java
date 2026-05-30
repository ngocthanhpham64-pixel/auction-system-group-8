package vn.edu.vnu.uet.group8.common.exception;

/**
 * Ngoại lệ xảy ra khi không tìm thấy vật phẩm (Item) trong hệ thống.
 * Thường được ném ra bởi tầng DAO hoặc Service khi truy vấn Database theo ID.
 */
public class ItemNotFoundException extends AuctionException {
  /**
   * Khởi tạo ngoại lệ với thông báo chi tiết.
   * @param message
   */
  public ItemNotFoundException(String message) {
    super(message);
  }

  /**
     * Khởi tạo ngoại lệ khi truy vấn Item thất bại.
     * @param itemId ID của vật phẩm không tồn tại.
     */
  public ItemNotFoundException(int itemId) {
    super("Không tìm thấy mặt hàng với ID: " + itemId);
  }
}
