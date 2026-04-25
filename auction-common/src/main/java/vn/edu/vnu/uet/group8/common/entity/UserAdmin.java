package vn.edu.vnu.uet.group8.common.entity;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import vn.edu.vnu.uet.group8.common.enums.AdminLevel;
import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;

public class UserAdmin extends User {
  private AdminLevel adminLevel;

  private UserAdmin(Builder b) {
    super(b);
    addRole(UserRole.ADMIN);
  }

  private UserAdmin(Reconstructor r) {
    super(r.id, r.createdAt, r.isDeleted, r.username, r.email, r.fullName,
          r.encryptedPassword, r.status, r.roles, r.lastLogin);
    this.adminLevel = r.adminLevel;
  }

  public static Reconstructor reconstructor() {
    return new Reconstructor();
  }

  // ════════════════════════════════════════════════════
  // RECONSTRUCTOR
  // ════════════════════════════════════════════════════
  public static class Reconstructor {
    private Integer id;
    private Instant createdAt;
    private Boolean isDeleted;
    private String username;
    private String email;
    private String fullName;
    private String encryptedPassword;
    private UserStatus status;
    private Set<UserRole> roles;
    private Instant lastLogin;
    private AdminLevel adminLevel;

    public Reconstructor id(int id) {
      this.id = id; return this;
    }
    public Reconstructor createdAt(Instant v) {
      this.createdAt = v; return this;
    }
    public Reconstructor isDeleted(boolean v) {
      this.isDeleted = v; return this;
    }
    public Reconstructor username(String v) {
      this.username = v; return this;
    }
    public Reconstructor email(String v) {
      this.email = v; return this;
    }
    public Reconstructor fullName(String v) {
      this.fullName = v; return this;
    }
    public Reconstructor encryptedPassword(String v) {
      this.encryptedPassword = v; return this;
    }
    public Reconstructor status(UserStatus v) {
      this.status = v; return this;
    }
    public Reconstructor roles(Set<UserRole> v) {
      this.roles = v; return this;
    }
    public Reconstructor lastLogin(Instant v) {
      this.lastLogin = v; return this;
    }
    public Reconstructor adminLevel(AdminLevel v) {
      this.adminLevel = v; return this;
    }

    public UserAdmin build() {
      requireNonNull(id, "id");
      requireNonNull(createdAt, "createdAt");
      requireNonNull(isDeleted, "isDeleted");
      requireNonNull(username, "username");
      requireNonNull(email, "email");
      requireNonNull(encryptedPassword, "encryptedPassword");
      requireNonNull(status, "status");
      requireNonNull(roles, "roles");
      requireNonNull(adminLevel, "adminLevel");
      
      return new UserAdmin(this);
    }

    private void requireNonNull(Object value, String fieldName) {
      if (value == null)
        throw new IllegalStateException(
          "Reconstructor thiếu field bắt buộc: [" + fieldName + "]. "
          + "Kiểm tra lại UserDAO.mapRow()");
    }
  }

  
  // ════════════════════════════════════════════════════
  // BUILDER
  // ════════════════════════════════════════════════════
  public static class Builder extends User.Builder<Builder> {
    private final AdminLevel adminLevel;

    public Builder(String username, String email, String encryptedPassword, AdminLevel adminLevel) {
      super(username, email, encryptedPassword);
      this.adminLevel = adminLevel;
      this.roles(EnumSet.of(UserRole.ADMIN));
    }

    /**
     * Override roles() — chặn không cho ai set role khác cho Admin.
     * Admin chỉ được có role ADMIN.
     */
    @Override
    public Builder roles(Set<UserRole> roles) {
      if (!roles.equals(EnumSet.of(UserRole.ADMIN))) {
        throw new IllegalArgumentException("Chỉ Admin set role");
      }
      return super.roles(roles);
    }

    @Override
    public UserAdmin build() {
      return new UserAdmin(this);
    }
  }
  // ════════════════════════════════════════════════════
  // OVERRIDE từ User
  // ════════════════════════════════════════════════════
  @Override
  public boolean isAdmin() {
    return true;
  }

  @Override
  public String getDisplayRole() {
    return adminLevel == AdminLevel.SUPER_ADMIN ? "SUPER_ADMIN" : "MODERATOR";
  }

  @Override
  public String toString() {
    return "AdminUser{" +
               "id='"          + getId()       + '\'' +
               ", username='"  + getUsername() + '\'' +
               ", adminLevel=" + adminLevel    +
               '}';
    }

  // ════════════════════════════════════════════════════
  // GETTERS
  // ════════════════════════════════════════════════════
  public boolean canBanUser() {
    return adminLevel == AdminLevel.SUPER_ADMIN;
  }

  public boolean canDeleteItem() {
    return true;
  }

  public AdminLevel getAdminLevel() {
    return adminLevel;
  }

  public boolean canApproveItem() {
    return true;
  }


}
