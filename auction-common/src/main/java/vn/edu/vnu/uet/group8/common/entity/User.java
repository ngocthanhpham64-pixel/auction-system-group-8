package vn.edu.vnu.uet.group8.common.entity;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;

/**
 * Lớp trừu tượng cơ sở cho tất cả các người dùng trên hệ thống.
 * Cung cấp các thuộc tính định danh và quản lý người dùng trên hệ thống.
 */
public abstract sealed class User extends Entity permits UserAdmin, UserMember{

  // ── Immutable sau khi tạo ────────────────────────────
  // Những field này không được phép thay đổi sau khi user đăng ký
  private final String username;
  private final String email;
  
  // ── Mutable có kiểm soát ─────────────────────────────
  // Những field này thay đổi qua method có validation
  private String fullname;
  private String encryptedPassword;
  private UserStatus status;
  private Instant lastLogin;
  private Set<UserRole> roles;

  /**
   * Constructor cho user MỚI
   * Chỉ Builder mới được gọi constructor này.
   */
  protected User(Builder<?> b) {
    this(0,
        Instant.now(),
        false,
        b.username,
        b.email,
        b.fullname,
        b.encryptedPassword,
        UserStatus.ACTIVE,
        b.roles,
        null
    );
  }

  /**
   * Constructor để NẠP LẠI từ DB — gọi Entity(id, createdAt, isDeleted).
   * Dùng khi khởi động server, load toàn bộ dữ liệu cũ lên memory.
   * Không validate — tin tưởng dữ liệu đã hợp lệ từ DB.
   */
  protected User(int id, Instant createdAt, boolean isDeleted,
                String username, String email, String fullname,
                String encryptedPassword, UserStatus status,
                Set<UserRole> roles, Instant lastLogin
                ) {
    super(id, createdAt, isDeleted);
    this.username          = username;
    this.email             = email;
    this.fullname          = fullname;
    this.encryptedPassword = encryptedPassword;
    this.status            = status;
    this.roles             = roles.isEmpty()
                              ? EnumSet.noneOf(UserRole.class)
                              : EnumSet.copyOf(roles);
    this.lastLogin         = lastLogin;
  }

  // ════════════════════════════════════════════════════
  // BUILDER
  // B extends Builder<B> là "self-type trick":
  // Mỗi method trả về B (kiểu thực của subclass Builder),
  // không phải User.Builder — nên chain method vẫn đúng kiểu.
  // ════════════════════════════════════════════════════

  public abstract static class Builder<B extends Builder<B>> {

    // Required — phải có khi gọi new Builder(...)
    private final String username;
    private final String email;
    private final String encryptedPassword; // Đã hash BCrypt trước khi vào đây

    // Optional — có giá trị mặc định
    private String fullname          = "";
    private Set<UserRole> roles      = EnumSet.of(UserRole.BIDDER);

    /**
     * Ba thứ bắt buộc khi tạo user mới:
     * username, email, encryptedPassword (đã BCrypt từ tầng Service).
     */
    public Builder(String username, String email,
                   String encryptedPassword) {
      if (username == null || username.isBlank())
        throw new IllegalArgumentException(
            "Username không được trống");
      if (email == null || !email.contains("@"))
        throw new IllegalArgumentException(
            "Email không hợp lệ");
      if (encryptedPassword == null || encryptedPassword.isBlank())
        throw new IllegalArgumentException(
            "Password không được trống");

      this.username          = username.trim().toLowerCase();
      this.email             = email.trim().toLowerCase();
      this.encryptedPassword = encryptedPassword;
    }

    /**
     * self() trả về đúng kiểu B của subclass.
     * Đây là cách duy nhất để chain method đúng kiểu
     * khi Builder có kế thừa.
     */
    @SuppressWarnings("unchecked")
    protected final B self() {
      return (B) this;
    }
    
    public B fullname(String fullname) {
      this.fullname = fullname != null ? fullname.trim() : "";
      return self();
    }

    public B roles(Set<UserRole> roles) {
      if (roles == null || roles.isEmpty())
        throw new IllegalArgumentException(
            "User phải có ít nhất một role");
      this.roles = EnumSet.copyOf(roles);
      return self();
    }

    public abstract User build();
  }

  // ════════════════════════════════════════════════════
  // GETTERS — tất cả field đều có getter
  // ════════════════════════════════════════════════════
  public String getUsername() {
    return username;
  }

  public String getEncryptedPassword() {
    return encryptedPassword;
  }

  public String getEmail() {
    return email;
  }

  public Instant getLastLogin() {
    return lastLogin;
  }

  public String getFullname() {
    return fullname;
  }

  public UserStatus getStatus() {
    return status;
  }
  
  /** Trả về bản sao để tránh bên ngoài modify trực tiếp Set */
  public Set<UserRole> getRoles() {
    return Collections.unmodifiableSet(roles);
  }

  // ════════════════════════════════════════════════════
  // SETTERS CÓ KIỂM SOÁT
  // Không phải setter thuần — mỗi cái có validation + lý do rõ ràng
  // ════════════════════════════════════════════════════
  /**
   * Chỉ cập nhật họ tên — thông tin profile không nhạy cảm.
   */
  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

   /**
   * Đổi mật khẩu — nhận vào password ĐÃ ĐƯỢC HASH ở tầng Service.
   * UserDAO không bao giờ hash — chỉ lưu những gì nhận được vào đây.
   */
  public void setEncryptedPassword(String encryptedPassword) {
    if (encryptedPassword == null || encryptedPassword.isBlank()) {
      throw new IllegalArgumentException(
        "Encrypted password không được trống");
    }
    this.encryptedPassword = encryptedPassword;
  }

  /**
   * Thay đổi trạng thái tài khoản.
   * Admin dùng để khoá/mở tài khoản, không để user tự gọi.
   */
  public void setStatus(UserStatus status) {
    if (status == null) {
      throw new IllegalArgumentException("Status không được null");
    }
    this.status = status;
  }

  /**
   * Ghi nhận thời điểm đăng nhập cuối.
   * Gọi ngay sau khi authenticate() thành công trong UserService.
   */
  public void recordLogin() {
    this.lastLogin = Instant.now();
  }

  /**
   * Cộng hoặc trừ balance — delta âm = trừ tiền, dương = cộng tiền.
   * Kiểm tra không để balance âm.
   */
  // public void adjustBalance(BigDecimal delta) {
  //   if (delta == null) {
  //     throw new IllegalArgumentException("Delta không được null");
  //   }
  //   BigDecimal newBalance = this.balance.add(delta);
  //   if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
  //     throw new IllegalStateException(
  //       "Số dư không đủ. Hiện có: " + this.balance
  //     );
  //   }
  //   this.balance = newBalance;
  // }

  /**
   * Thêm role — khi user đăng item đầu tiên, tự động thêm SELLER.
   */
  public void setRoles(UserRole role) {
    if (roles == null) {
      throw new IllegalArgumentException(
        "Roll không được null"
      );
    }
    this.roles.add(role);
  }

  /**
   * Xoá role — khi admin thu hồi quyền bán hàng chẳng hạn.
   * Không cho phép xoá hết toàn bộ roles.
   */
  public void removeRole(UserRole role) {
    if (roles.size() <= 1) {
      throw new IllegalStateException(
        "User phải có ít nhất một role"
      );
    }
    this.roles.remove(role);
    }

  // ════════════════════════════════════════════════════
  // HELPER — logic nghiệp vụ thường dùng
  // ════════════════════════════════════════════════════
  public boolean isActive() {
    return status == UserStatus.ACTIVE && !isDeleted();
  }

  public boolean hasRole(UserRole role) {
    return roles.contains(role);
  }

  public boolean canBid(UserRole role) {
    return role == UserRole.BIDDER && this.status == UserStatus.ACTIVE;
  }

  public boolean canSell(UserRole role) {
    return role == UserRole.SELLER && this.status == UserStatus.ACTIVE;
  }

  public void addRole(UserRole role) {
    roles.add(role);
  }

  // ════════════════════════════════════════════════════
  // Abstract method
  // ════════════════════════════════════════════════════
  public abstract boolean isAdmin();
  public abstract String getDisplayRole();

  // ════════════════════════════════════════════════════
  // OVERRIDE từ Entity
  // ════════════════════════════════════════════════════  
  @Override
  public String toString() {
    return "User{" +
               "id='"       + getId()     + '\'' +
               ", username='" + username  + '\'' +
               ", email='"    + email     + '\'' +
               ", status="    + status    +
               ", roles="     + roles     +
               ", lastLogin=" + lastLogin +
               ", createdAt=" + getCreatedAt() +
               ", isDeleted=" + isDeleted() +
               '}';
  }
}
