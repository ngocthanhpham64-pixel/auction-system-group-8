package vn.edu.vnu.uet.group8.common.entity;

import java.time.LocalDateTime;

public abstract class User extends Entity {
  /**
   * Lớp trừu tượng cơ sở cho tất cả các người dùng trên hệ thống.
   * Cung cấp các thuộc tính định danh và quản lý người dùng trên hệ thống.
   */

  private String username;
  private String fullName;
  private String encryptedPassword;
  private String email;
  private LocalDateTime time;

  /**
   * Constructor mặc định cho User mới.
   */

  public User() {
    super();
  }

  /**
   * Constructor đầy đủ cho User, thường dùng khi nạp dữ liệu từ database.
   * 
   * @param id
   * @param createdAt
   * @param isDeleted
   * @param username
   * @param fullName
   * @param encryptedPassword
   * @param email
   * @param time
   */

  public User(String id, LocalDateTime createdAt, boolean isDeleted, String username, String fullName, String encryptedPassword, String email, LocalDateTime time) {
    super(id, createdAt, isDeleted);
    this.username = username;
    this.fullName = fullName;
    this.encryptedPassword = encryptedPassword;
    this.email = email;
    this.time = time;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEncryptedPassword() {
    return encryptedPassword;
  }

  public void setEncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Mỗi lớp con (Bidder, Seller, Admin) sẽ trả về một chuỗi định danh view khác nhau.
   *
   * @return Tên của file giao diện (FXML) hoặc ID của View.
   */

  public abstract String getDashboardView();

  /**
   * Trả về vai trò cụ thể của người dùng dưới dạng Enum.
   */
  public abstract UserRole getRole();

  @Override
  public String toString() {
    return "User{" +
            "username='" + username + '\'' +
            ", fullName='" + fullName + '\'' +
            ", email='" + email + '\'' +
            '}';
  }
}
