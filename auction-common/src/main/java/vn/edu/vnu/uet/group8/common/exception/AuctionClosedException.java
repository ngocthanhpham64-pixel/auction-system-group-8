package vn.edu.vnu.uet.group8.common.exception;

/**
 * Ngoại lệ xảy ra khi người dùng cố gắng đặt giá vào một phiên đấu giá không ở trạng thái RUNNING.
 */
public class AuctionClosedException extends AuctionException {
  public AuctionClosedException(String message) {
    super(message);
  }
}