// ── Đăng nhập sai ─────────────────────────────────────────
package vn.edu.vnu.uet.group8.common.exception;

public class InvalidCredentialsException
        extends UserServiceException {
    public InvalidCredentialsException() {
        // Cùng một message cho cả "email sai" và "password sai"
        // Tránh lộ thông tin "email này có tồn tại không"
        super("Email hoặc mật khẩu không chính xác");
    }
}