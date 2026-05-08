package vn.edu.vnu.uet.group8.common.entity;

import vn.edu.vnu.uet.group8.common.enums.UserRole;

import java.math.BigDecimal;
import java.time.Instant;

public class User {
  private int id; // 0 = chưa persisted
  private final String username;
  private final String email;
  private final String encryptedPassword;
  private final String fullName;
  private final String phone;
  private final UserRole role;
  private final BigDecimal balance;
  private final Instant createdAt;
  private final boolean isDeleted;

  private User() { /* Gson */
    this.username = null; this.email = null;
    this.encryptedPassword = null; this.fullName = null;
    this.phone = null; this.role = null;
    this.balance = null; this.createdAt = null;
    this.isDeleted = false;
  }

  private User(Builder b) {
    this.username          = b.username;
    this.email             = b.email;
    this.encryptedPassword = b.encryptedPassword;
    this.fullName          = b.fullName;
    this.phone             = b.phone;
    this.role              = b.role;
    this.balance           = b.balance;
    this.createdAt         = b.createdAt;
    this.isDeleted         = b.isDeleted;
  }

  // Tạo mới (Service gọi khi đăng ký)
  public static Builder builder(String username, String email, String encryptedPassword) {
    return new Builder(username, email, encryptedPassword);
  }

  // Tái tạo từ DB (DAO gọi trong mapRow)
  public static Reconstructor reconstruct() {
    return new Reconstructor();
  }

  public boolean isPersisted() { return id > 0; }

  public void assignId(int id) {
    if (this.id != 0) throw new IllegalStateException("ID đã được gán");
    if (id <= 0)      throw new IllegalArgumentException("ID phải > 0");
    this.id = id;
  }

  // Getters
  public int getId()                    { return id; }
  public String getUsername()           { return username; }
  public String getEmail()              { return email; }
  public String getEncryptedPassword()  { return encryptedPassword; }
  public String getFullName()           { return fullName; }
  public String getPhone()              { return phone; }
  public UserRole getRole()             { return role; }
  public BigDecimal getBalance()        { return balance; }
  public Instant getCreatedAt()         { return createdAt; }
  public boolean isDeleted()            { return isDeleted; }
  public static class Builder {
    private final String username, email, encryptedPassword;
    private String fullName, phone;
    private UserRole role = UserRole.MEMBER; // mặc định
    private BigDecimal balance = BigDecimal.ZERO;
    private Instant createdAt = Instant.now();
    private boolean isDeleted = false;

    private Builder(String username, String email, String encryptedPassword) {
      // validation bắt buộc
      if (username == null || username.trim().isEmpty()) throw new IllegalArgumentException("Username không được rỗng");
      if (email == null || !email.matches("^[^@]+@[^@]+\\.[^@]+$")) throw new IllegalArgumentException("Email không hợp lệ");
      if (encryptedPassword == null || encryptedPassword.isEmpty()) throw new IllegalArgumentException("Password hash không được rỗng");
      this.username = username.trim().toLowerCase(); // chuẩn hóa
      this.email = email.trim().toLowerCase();
      this.encryptedPassword = encryptedPassword;
    }

    public Builder fullName(String fullName) { this.fullName = fullName; return this; }
    public Builder phone(String phone) { this.phone = phone; return this; }
    public Builder role(UserRole role) { this.role = role; return this; }
    public Builder balance(BigDecimal balance) { this.balance = balance; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder isDeleted(boolean isDeleted) { this.isDeleted = isDeleted; return this; }

    public User build() {
      return new User(this);
    }
  }
  public static class Reconstructor {
    private int id;
    private String username, email, encryptedPassword, fullName, phone;
    private UserRole role;
    private BigDecimal balance;
    private Instant createdAt;
    private boolean isDeleted;

    private Reconstructor() {}

    public Reconstructor id(int id) { this.id = id; return this; }
    public Reconstructor username(String username) { this.username = username; return this; }
    public Reconstructor email(String email) { this.email = email; return this; }
    public Reconstructor encryptedPassword(String pwd) { this.encryptedPassword = pwd; return this; }
    public Reconstructor fullName(String fullName) { this.fullName = fullName; return this; }
    public Reconstructor phone(String phone) { this.phone = phone; return this; }
    public Reconstructor role(UserRole role) { this.role = role; return this; }
    public Reconstructor balance(BigDecimal balance) { this.balance = balance; return this; }
    public Reconstructor createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Reconstructor isDeleted(boolean isDeleted) { this.isDeleted = isDeleted; return this; }

    public User build() {
      // Gọi constructor của User nhận Reconstructor? Hoặc dùng Builder nhưng không validate.
      // Cách gọn: User có thêm constructor nhận Reconstructor.
      return new User(this);
    }
  }
}