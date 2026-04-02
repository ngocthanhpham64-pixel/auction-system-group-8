package vn.edu.vnu.uet.group8.common.exception;

/**
 * Ngoại lệ cơ sở (Base Exception) cho toàn bộ hệ thống đấu giá.
 * Tất cả các ngoại lệ tùy chỉnh trong dự án đều phải kế thừa từ lớp này.
 * <p>Sử dụng {@link RuntimeException} để tránh việc phải khai báo 'throws' quá nhiều,
 * giúp code sạch hơn (Unchecked Exception).</p>
 *
 */
public class AuctionException extends RuntimeException {
  /**
     * Khởi tạo một ngoại lệ đấu giá với thông báo chi tiết.
     * @param message Thông báo mô tả nguyên nhân gây lỗi.
     */
   public AuctionException(String message) { 
      super(message);
    }
}
