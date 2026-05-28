package vn.edu.vnu.uet.group8.common.dto.model;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * DTO mô tả gọn 1 người dùng để hiển thị trong table quản lý admin.
 *
 * <h3>FIX so với bản cũ:</h3>
 * <ul>
 *   <li>Field cũ: <code>private String userName</code> → JSON ra <code>"userName"</code></li>
 *   <li>Field mới: <code>private String username</code> + <code>@SerializedName("username")</code></li>
 *   <li>→ Khớp với JSON convention <code>username</code> chuẩn từ BE</li>
 * </ul>
 *
 * <h3>Đặc điểm thiết kế:</h3>
 * <ul>
 *   <li><b>Constants cho role/status</b> - tránh typo, dễ refactor sang enum</li>
 *   <li><b>Helper isAdmin/isBanned/isActive</b> - logic gom 1 chỗ</li>
 *   <li><b>Null-safe getter</b> - các trường String trả "" thay null</li>
 *   <li><b>toString/equals/hashCode</b> - dễ log + test</li>
 * </ul>
 *
 * <h3>Roles:</h3>
 * <ul>
 *   <li>{@link #ROLE_MEMBER} - người dùng thường, có thể bid</li>
 *   <li>{@link #ROLE_SELLER} - người bán, đăng được sản phẩm</li>
 *   <li>{@link #ROLE_ADMIN} - moderator, quản lý user và auction</li>
 *   <li>{@link #ROLE_SUPER_ADMIN} - super admin, full quyền</li>
 * </ul>
 *
 * <h3>Statuses:</h3>
 * <ul>
 *   <li>{@link #STATUS_ACTIVE} - bình thường</li>
 *   <li>{@link #STATUS_SUSPENDED} - bị khóa tạm thời</li>
 *   <li>{@link #STATUS_BANNED} - bị cấm vĩnh viễn</li>
 * </ul>
 */
public class UserAdminDTO {

    // ===== Role constants =====
    public static final String ROLE_MEMBER      = "MEMBER";
    public static final String ROLE_SELLER      = "SELLER";
    public static final String ROLE_ADMIN       = "ADMIN";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    // ===== Status constants =====
    public static final String STATUS_ACTIVE    = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_BANNED    = "BANNED";

    private int id;

    // FIX: field 'userName' (camelCase với N hoa) → 'username' (chuẩn JSON)
    @SerializedName("username")
    private String username;

    private String email;

    /** Vai trò: MEMBER / SELLER / ADMIN / SUPER_ADMIN */
    private String role;

    /** Trạng thái: ACTIVE / SUSPENDED / BANNED */
    private String status;

    /** No-arg constructor cho Gson. */
    public UserAdminDTO() {}

    public UserAdminDTO(int id, String username, String email, String role, String status) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    // ===== Getters (null-safe) =====

    public int getId() { return id; }

    public String getUsername() {
        return username != null ? username : "";
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public String getRole() {
        return role != null ? role : "";
    }

    public String getStatus() {
        return status != null ? status : "";
    }

    // ===== Setters =====

    public void setId(int id)                  { this.id = id; }
    public void setUsername(String username)   { this.username = username; }
    public void setEmail(String email)         { this.email = email; }
    public void setRole(String role)           { this.role = role; }
    public void setStatus(String status)       { this.status = status; }

    // ===== Role helpers =====

    /** Có quyền admin (bao gồm cả super admin)? */
    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_SUPER_ADMIN.equalsIgnoreCase(role);
    }

    public boolean isSuperAdmin() {
        return ROLE_SUPER_ADMIN.equalsIgnoreCase(role);
    }

    public boolean isSeller() {
        return ROLE_SELLER.equalsIgnoreCase(role);
    }

    // ===== Status helpers =====

    public boolean isActive()    { return STATUS_ACTIVE.equalsIgnoreCase(status); }
    public boolean isSuspended() { return STATUS_SUSPENDED.equalsIgnoreCase(status); }
    public boolean isBanned()    { return STATUS_BANNED.equalsIgnoreCase(status); }

    /** User có thể đăng nhập / hoạt động không. */
    public boolean canLogin() {
        return isActive();
    }

    // ===== Object overrides =====

    @Override
    public String toString() {
        return "UserAdminDTO{" +
                "id=" + id +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", role='" + getRole() + '\'' +
                ", status='" + getStatus() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAdminDTO that)) return false;
        return id == that.id;  // id là khóa chính
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}