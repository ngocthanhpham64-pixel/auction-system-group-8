package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;

import vn.edu.vnu.uet.group8.common.entity.UserAdmin;
import vn.edu.vnu.uet.group8.common.entity.UserMember;
import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;

/**
 * DTO đầy đủ — dùng khi Client xem trang profile của chính mình
 * hoặc Admin xem thông tin user khác.
 *
 * Hai trường hợp dùng khác nhau:
 *
 *   UserMember xem profile của mình:
 *     → có balance, sellerRating, phone, stats
 *     → KHÔNG có adminLevel
 *
 *   Admin xem profile của user khác:
 *     → có status, createdAt để quản lý
 *     → KHÔNG có balance (admin không cần biết số dư user)
 *
 * Giải pháp: một DTO duy nhất, field nào không dùng thì null.
 * Client kiểm tra isAdmin để quyết định hiển thị phần nào.
 *
 * KHÔNG chứa tuyệt đối:
 *   encryptedPassword  — không bao giờ gửi về Client
 *   isDeleted          — thông tin nội bộ Server
 */
public class UserProfileDTO {

  // ── Định danh ────────────────────────────────────────
  private int    userId;
  private String username;
  private String email;

  // ── Thông tin cá nhân (UserMember) ───────────────────
  private String fullName;   // null nếu là UserAdmin
  private String phone;      // null nếu chưa điền hoặc UserAdmin
  private String avatarUrl;

  // ── Tài chính (chỉ UserMember xem profile của chính mình) ──
  // null nếu Admin xem — Admin không cần biết số dư user
  private BigDecimal balance;
  private BigDecimal sellerRating;  // null = chưa có đánh giá nào

  // ── Thống kê (UserMember) ─────────────────────────────
  private int totalBidsPlaced;
  private int totalItemsSold;

  // ── Trạng thái & Phân quyền ───────────────────────────
  private UserStatus status;
  private String     displayRole;
  private boolean    isAdmin;

  // ── Admin-only (null nếu là UserMember) ──────────────
  private AdminLevel adminLevel;   // MODERATOR / SUPER_ADMIN

  // ── Timestamps ────────────────────────────────────────
  private Instant createdAt;
  private Instant lastLogin;  // null = chưa login lần nào

  // Constructor private
  private UserProfileDTO() {}

  // ════════════════════════════════════════════════════
  // STATIC FACTORY METHODS
  // ════════════════════════════════════════════════════

  /**
   * UserMember xem profile của chính mình.
   * Bao gồm balance và thông tin tài chính.
   * Server gọi khi xác nhận request đến từ chính user đó.
   */
  public static UserProfileDTO fromMember(UserMember user) {
      UserProfileDTO dto = new UserProfileDTO();
      dto.userId           = user.getId();
      dto.username         = user.getUsername();
      dto.email            = user.getEmail();
      dto.fullName         = user.getFullname();
      dto.phone            = user.getPhone();
      dto.avatarUrl        = user.getAvatarUrl();
      dto.balance          = user.getBalance();
      dto.sellerRating     = user.getSellerRating();
      dto.totalBidsPlaced  = user.getTotalBidsPlaced();
      dto.totalItemsSold   = user.getTotalItemsSold();
      dto.status           = user.getStatus();
      dto.displayRole      = user.getDisplayRole();
      dto.isAdmin          = false;
      dto.createdAt        = user.getCreatedAt();
      dto.lastLogin        = user.getLastLogin();

      // Admin fields: null — không cần thiết
      dto.adminLevel  = null;
      return dto;
  }

  /**
   * Admin xem profile của bất kỳ UserMember nào.
   * KHÔNG bao gồm balance — Admin không cần biết số dư.
   * Server gọi khi xác nhận request đến từ Admin.
   */
  public static UserProfileDTO fromMemberForAdmin(UserMember user) {
      UserProfileDTO dto = fromMember(user);
      dto.balance = null; // che số dư khi Admin xem
      return dto;
  }

  /**
   * Thông tin UserAdmin — dùng khi Super Admin xem profile
   * của Moderator khác, hoặc Admin xem profile của chính mình.
   */
  public static UserProfileDTO fromAdmin(UserAdmin user) {
    UserProfileDTO dto = new UserProfileDTO();
    dto.userId      = user.getId();
    dto.username    = user.getUsername();
    dto.email       = user.getEmail();
    dto.status      = user.getStatus();
    dto.displayRole = user.getDisplayRole();
    dto.isAdmin     = true;
    dto.adminLevel  = user.getAdminLevel();
    dto.createdAt   = user.getCreatedAt();
    dto.lastLogin   = user.getLastLogin();

    // Member fields: null — UserAdmin không có những thứ này
    dto.fullName        = null;
    dto.phone           = null;
    dto.balance         = null;
    dto.sellerRating    = null;
    return dto;
  }

  /**
   * Tạo DTO rút gọn cho người dùng khác xem.
   * Chỉ bao gồm các thông tin công khai như username, fullName, rating và stats.
   * KHÔNG bao gồm email, phone, balance, status, adminLevel.
   */
  public static UserProfileDTO fromMemberForOther(UserMember user) {
    UserProfileDTO dto = new UserProfileDTO();
    dto.userId           = user.getId();
    dto.username         = user.getUsername();
    dto.fullName         = user.getFullname();
    dto.sellerRating     = user.getSellerRating();
    dto.avatarUrl        = user.getAvatarUrl();
    dto.totalItemsSold   = user.getTotalItemsSold();
    dto.displayRole      = user.getDisplayRole();
    dto.isAdmin          = false;
    dto.createdAt        = user.getCreatedAt();

    // Bảo mật: Ẩn các thông tin nhạy cảm/nội bộ
    dto.email            = null;
    dto.phone            = null;
    dto.balance          = null;
    dto.status           = null;
    dto.lastLogin        = null;
    dto.adminLevel       = null;
    dto.totalBidsPlaced  = 0;
    
    return dto;
  }

  // ════════════════════════════════════════════════════
  // GETTERS
  // ════════════════════════════════════════════════════

  public int        getUserId()          {
    return userId;
  }
  public String     getUsername()        {
    return username;
  }
  public String     getEmail()           {
    return email;
  }
  public String     getFullName()        {
    return fullName;
  }
  public String     getAvatarUrl()       {
    return avatarUrl;
  }
  public String     getPhone()           {
    return phone;
  }
  public BigDecimal getBalance()         {
    return balance;
  }
  public BigDecimal getSellerRating()    {
    return sellerRating;
  }
  public int        getTotalBidsPlaced() {
    return totalBidsPlaced;
  }
  public int        getTotalItemsSold()  {
    return totalItemsSold;
  }
  public UserStatus getStatus()          {
    return status;
  }
  public String     getDisplayRole()     {
    return displayRole;
  }
  public boolean    isAdmin()            {
    return isAdmin;
  }
  public AdminLevel getAdminLevel()      {
    return adminLevel;
  }
  public Instant    getCreatedAt()       {
    return createdAt;
  }
  public Instant    getLastLogin()       {
    return lastLogin;
  }

  // ── Helper queries — Client dùng cho UI logic ────────

  /** Tài khoản đang hoạt động không — hiển thị badge status */
  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }

  /** Có phải seller không — hiển thị tab "Sản phẩm đang bán" */
  public boolean isSeller() {
    return totalItemsSold > 0
        || "Người bán".equals(displayRole)
        || "Người mua & Người bán".equals(displayRole);
  }

  /** Có rating chưa — tránh hiển thị "0.0 sao" khi chưa ai đánh giá */
  public boolean hasRating() {
    return sellerRating != null;
  }

  @Override
  public String toString() {
    return "UserProfileDTO{" +
            "userId="        + userId        +
            ", username='"   + username      + '\'' +
            ", isAdmin="     + isAdmin       +
            ", status="      + status        +
            ", lastLogin="   + lastLogin     +
            '}';
  }
}