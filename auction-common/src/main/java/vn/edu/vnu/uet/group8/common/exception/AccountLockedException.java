// ── Tài khoản bị khoá ────────────────────────────────────
package vn.edu.vnu.uet.group8.common.exception;

import vn.edu.vnu.uet.group8.common.enums.UserStatus;

public class AccountLockedException extends UserServiceException {
  private final UserStatus status;

  public AccountLockedException(UserStatus status) {
    super(status == UserStatus.BANNED
        ? "Tài khoản đã bị cấm vĩnh viễn"
        : "Tài khoản đang bị tạm khoá. "
        + "Liên hệ admin để được hỗ trợ");
    this.status = status;
  }

  public UserStatus getStatus() {
    return status;
  }
}