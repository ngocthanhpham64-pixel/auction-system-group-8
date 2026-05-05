package vn.edu.vnu.uet.group8.common.dto.model;

import vn.edu.vnu.uet.group8.common.entity.User;
import vn.edu.vnu.uet.group8.common.entity.UserMember;

/**
 * DTO gọn nhất — nhúng trong AuthResponse sau khi login thành công.
 * Chỉ chứa đủ thông tin để Client:
 *   1. Hiển thị tên người dùng trên header
 *   2. Quyết định hiển thị menu Admin hay không
 *   3. Lưu session (userId dùng cho mọi request tiếp theo)
 *
 * KHÔNG chứa: password, balance, phone, isDeleted, createdAt
 * — Client không cần những thứ này ngay sau login.
 */
public class UserSummaryDTO {

  // ── Định danh ────────────────────────────────────────
  private int    userId;
  private String username;

  // ── Hiển thị UI ──────────────────────────────────────
  private String  displayName;   // fullName nếu có, fallback username
  private String  displayRole;   // "Người mua & Người bán" / "Admin"
  private boolean isAdmin;       // true → hiện menu admin trên UI

  // Constructor private
  private UserSummaryDTO() {}

  // ════════════════════════════════════════════════════
  // STATIC FACTORY
  // ════════════════════════════════════════════════════

  /**
   * Tạo từ bất kỳ loại User nào — Service gọi sau authenticate().
   * Dùng pattern matching (instanceof) để lấy đúng thông tin.
   */
  public static UserSummaryDTO from(User user) {
    UserSummaryDTO dto = new UserSummaryDTO();
    dto.userId      = user.getId();
    dto.username    = user.getUsername();
    dto.isAdmin     = user.isAdmin();
    dto.displayRole = user.getDisplayRole();

    // displayName: ưu tiên fullname của MemberUser
    // AdminUser không có fullname → fallback về username
    if (user instanceof UserMember m
            && m.getFullname() != null
            && !m.getFullname().isBlank()) {
        dto.displayName = m.getFullname();
    } else {
        dto.displayName = user.getUsername();
    }

    return dto;
  }

  // Getters
  public int     getUserId()     { return userId; }
  public String  getUsername()   { return username; }
  public String  getDisplayName(){ return displayName; }
  public String  getDisplayRole(){ return displayRole; }
  public boolean isAdmin()       { return isAdmin; }

  @Override
  public String toString() {
      return "UserSummaryDTO{" +
              "userId="       + userId      +
              ", username='"  + username    + '\'' +
              ", displayRole='" + displayRole + '\'' +
              ", isAdmin="    + isAdmin     +
              '}';
  }
}
