// ── Không đủ quyền ────────────────────────────────────────
package vn.edu.vnu.uet.group8.common.exception;

public class UnauthorizedException extends UserServiceException {
  public UnauthorizedException(String action) {
    super("Không có quyền thực hiện: " + action);
  }
}