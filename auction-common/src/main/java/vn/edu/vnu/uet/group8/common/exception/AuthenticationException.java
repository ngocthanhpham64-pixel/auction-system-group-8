package vn.edu.vnu.uet.group8.common.exception;

/**
 * Ngoại lệ xảy ra khi người dùng chưa đăng nhập hoặc không có quyền thực hiện hành động.
 */
public class AuthenticationException extends AuctionException {
  public AuthenticationException(String message) {
    super(message);
  }
}