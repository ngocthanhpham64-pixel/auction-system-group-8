// ── Input không hợp lệ ────────────────────────────────────
package vn.edu.vnu.uet.group8.common.exception;

public class ValidationException extends UserServiceException {
  public ValidationException(String message) {
    super(message);
  }
}