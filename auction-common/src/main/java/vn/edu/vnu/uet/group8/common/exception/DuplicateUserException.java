// ── Trùng username hoặc email ─────────────────────────────
package vn.edu.vnu.uet.group8.common.exception;

public class DuplicateUserException extends UserServiceException {
  public DuplicateUserException(String field, String value) {
    super(field + " '" + value + "' đã được sử dụng");
  }
}