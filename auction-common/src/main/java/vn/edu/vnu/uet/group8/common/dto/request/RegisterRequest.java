package vn.edu.vnu.uet.group8.common.dto.request;

import com.google.gson.annotations.SerializedName;

/**
 * Đối tượng yêu cầu đăng ký người dùng.
 * Tuân thủ Google Java Style: fields bất biến (final), 2-space indent, fluent builder.
 */
public final class RegisterRequest {

  // --- Constants (Hằng số định nghĩa quy tắc) ---
  private static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
  private static final String PHONE_REGEX = "^(0|\\+84)(\\d{9})$";

  // --- Fields (Bất biến) ---
  @SerializedName("username")
  private final String username;

  @SerializedName("password")
  private final String password;

  @SerializedName("email")
  private final String email;

  @SerializedName("fullname")
  private final String fullname;

  @SerializedName("phone")
  private final String phone;

  @SerializedName("address")
  private final String address;

  /** Constructor rỗng cho GSON (Reflection). */
  private RegisterRequest() {
    this.username = null;
    this.password = null;
    this.email = null;
    this.fullname = null;
    this.phone = null;
    this.address = null;
  }

  /** Constructor chính thông qua Builder. */
  private RegisterRequest(Builder builder) {
    this.username = builder.username;
    this.password = builder.password;
    this.email = builder.email;
    this.fullname = builder.fullname;
    this.phone = builder.phone;
    this.address = builder.address;
  }

  /**
   * Kiểm tra tính hợp lệ của toàn bộ dữ liệu.
   * @throws IllegalArgumentException nếu bất kỳ trường nào vi phạm quy tắc.
   */
  public void validate() {
    checkRequired(username, "Tên đăng nhập không được để trống");
    checkMinLength(username, 3, "Tên đăng nhập tối thiểu 3 ký tự");

    checkRequired(password, "Mật khẩu không được để trống");
    checkMinLength(password, 6, "Mật khẩu tối thiểu 6 ký tự");

    checkRequired(email, "Email không được để trống");
    checkRegex(email, EMAIL_REGEX, "Email không đúng định dạng");

    checkRequired(phone, "Số điện thoại không được để trống");
    checkRegex(phone, PHONE_REGEX, "Số điện thoại không hợp lệ (cần 10 số)");
  }

  // --- Private Helpers cho Validation ---

  private void checkRequired(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }

  private void checkMinLength(String value, int min, String message) {
    if (value != null && value.length() < min) {
      throw new IllegalArgumentException(message);
    }
  }

  private void checkRegex(String value, String regex, String message) {
    if (value != null && !value.matches(regex)) {
      throw new IllegalArgumentException(message);
    }
  }

  // --- Getters ---

  public String getUsername() { return username; }
  public String getPassword() { return password; }
  public String getEmail() { return email; }
  public String getFullname() { return fullname; }
  public String getPhone() { return phone; }
  public String getAddress() { return address; }

  @Override
  public String toString() {
    return String.format(
        "RegisterRequest{username='%s', email='%s', fullname='%s', password='[PROTECTED]'}",
        username, email, fullname);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Lớp Builder để khởi tạo RegisterRequest một cách an toàn. */
  public static class Builder {
    private String username;
    private String password;
    private String email;
    private String fullname;
    private String phone;
    private String address;

    public Builder username(String username) {
      this.username = normalizeLower(username);
      return this;
    }

    public Builder password(String password) {
      this.password = password; // Mật khẩu giữ nguyên, không trim hay lower
      return this;
    }

    public Builder email(String email) {
      this.email = normalizeLower(email);
      return this;
    }

    public Builder fullname(String fullname) {
      this.fullname = normalize(fullname);
      return this;
    }

    public Builder phone(String phone) {
      this.phone = normalize(phone);
      return this;
    }

    public Builder address(String address) {
      this.address = normalize(address);
      return this;
    }

    private String normalize(String s) {
      return s != null ? s.trim() : null;
    }

    private String normalizeLower(String s) {
      return s != null ? s.trim().toLowerCase() : null;
    }

    public RegisterRequest build() {
      RegisterRequest request = new RegisterRequest(this);
      request.validate();
      return request;
    }
  }
}