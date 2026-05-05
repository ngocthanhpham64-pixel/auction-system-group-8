package vn.edu.vnu.uet.group8.common.dto.request;

import com.google.gson.annotations.SerializedName;

public final class LoginRequest {

  @SerializedName("username")
  private final String username;

  @SerializedName("password")
  private final String password;

  // 1. Constructor rỗng cho GSON (BẮT BUỘC để Server không dùng Unsafe)
  // Để private để không ai gọi được ngoài GSON
  private LoginRequest() {
    this.username = null;
    this.password = null;
  }

  // 2. Constructor tư nhân cho Builder
  private LoginRequest(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public static class Builder {
    private String username;
    private String password;

    public Builder username(String username) {
      // Chuẩn hóa NGAY LÚC NHẬN
      this.username = (username != null) ? username.trim().toLowerCase() : null;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public LoginRequest build() {
      validate();
      return new LoginRequest(username, password);
    }

    private void validate() {
      if (username == null || username.isEmpty()) {
        throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
      }
      if (password == null || password.isEmpty()) {
        throw new IllegalArgumentException("Mật khẩu không được để trống.");
      }
    }
  }

  // ════════════════════════════════════════════════════
  // GETTERS
  // ════════════════════════════════════════════════════

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String toString() {
    return "LoginRequest{" +
            "username='" + username + '\'' +
            ", password='" + (password == null ? "null" : "*****") + '\'' +
            '}';
  }
}