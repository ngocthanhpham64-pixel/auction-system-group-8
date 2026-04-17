package vn.edu.vnu.uet.group8.common.exception;
/**
 * Ngoại lệ xảy ra khi giá đặt không hợp lệ (thấp hơn hoặc bằng giá hiện tại, hoặc tự bid đè).
 */
public class InvalidBidException extends AuctionException {
  public InvalidBidException(String message) {
    super(message);
  }
}
