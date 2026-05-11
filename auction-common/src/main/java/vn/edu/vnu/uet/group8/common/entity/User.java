package vn.edu.vnu.uet.group8.common.entity;

import vn.edu.vnu.uet.group8.common.enums.UserRole;
import java.math.BigDecimal;
import java.time.Instant;

public class User {

  private int id;
  private final String username;
  private final String email;
  private final String encryptedPassword;
  private final String fullName;
  private final String phone;
  private final UserRole role;
  private final BigDecimal balance;
  private final Instant createdAt;
  private final boolean isDeleted;

  // No-arg cho GSON
  private User() {
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

  // ✅ Fix Lỗi 2: thêm constructor nhận Reconstructor
  private User(Reconstructor r) {
    this.id                = r.id;
    this.username          = r.username;
    this.email             = r.email;
    this.encryptedPassword = r.encryptedPassword;
    this.fullName          = r.fullName;
    this.phone             = r.phone;
    this.role              = r.role;
    this.balance           = r.balance;
    this.createdAt         = r.createdAt;
    this.isDeleted         = r.isDeleted;
  }

  // Factory methods
  public static Builder builder(String username, String email, String encryptedPassword) {
    return new Builder(username, email, encryptedPassword);
  }

  // ✅ Fix Lỗi 1: XÓA dòng builder() no-arg — Builder không thể tạo không có 3 tham số bắt buộc
  // public static Builder builder() { return new Builder(); } ← ĐÃ XÓA

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
  public int        getId()                   { return id;                }
  public String     getUsername()             { return username;          }
  public String     getEmail()                { return email;             }
  public String     getEncryptedPassword()    { return encryptedPassword; }
  public String     getFullName()             { return fullName;          }
  public String     getPhone()                { return phone;             }
  public UserRole   getRole()                 { return role;              }
  public BigDecimal getBalance()              { return balance;           }
  public Instant    getCreatedAt()            { return createdAt;         }
  public boolean    isDeleted()               { return isDeleted;         }

  // ── Builder ──────────────────────────────────────────────────────────────

  public static class Builder {
    private final String username, email, encryptedPassword;
    private String   fullName, phone;
    private UserRole role      = UserRole.MEMBER;
    private BigDecimal balance = BigDecimal.ZERO;
    private Instant createdAt  = Instant.now();
    private boolean isDeleted  = false;

    private Builder(String username, String email, String encryptedPassword) {
      if (username == null || username.trim().isEmpty())
        throw new IllegalArgumentException("Username không được rỗng");
      if (email == null || !email.matches("^[^@]+@[^@]+\\.[^@]+$"))
        throw new IllegalArgumentException("Email không hợp lệ");
      if (encryptedPassword == null || encryptedPassword.isEmpty())
        throw new IllegalArgumentException("Password hash không được rỗng");
      this.username          = username.trim().toLowerCase();
      this.email             = email.trim().toLowerCase();
      this.encryptedPassword = encryptedPassword;
    }

    public Builder fullName(String v)        { this.fullName  = v; return this; }
    public Builder phone(String v)           { this.phone     = v; return this; }
    public Builder role(UserRole v)          { this.role      = v; return this; }
    public Builder balance(BigDecimal v)     { this.balance   = v; return this; }
    public Builder createdAt(Instant v)      { this.createdAt = v; return this; }
    public Builder isDeleted(boolean v)      { this.isDeleted = v; return this; }
    public User build()                      { return new User(this); }
  }

  // ── Reconstructor ────────────────────────────────────────────────────────

  public static class Reconstructor {
    private int id;
    private String username, email, encryptedPassword, fullName, phone;
    private UserRole   role;
    private BigDecimal balance;
    private Instant    createdAt;
    private boolean    isDeleted;

    private Reconstructor() {}

    public Reconstructor id(int v)                    { this.id                = v; return this; }
    public Reconstructor username(String v)           { this.username          = v; return this; }
    public Reconstructor email(String v)              { this.email             = v; return this; }
    public Reconstructor encryptedPassword(String v)  { this.encryptedPassword = v; return this; }
    public Reconstructor fullName(String v)           { this.fullName          = v; return this; }
    public Reconstructor phone(String v)              { this.phone             = v; return this; }
    public Reconstructor role(UserRole v)             { this.role              = v; return this; }
    public Reconstructor balance(BigDecimal v)        { this.balance           = v; return this; }
    public Reconstructor createdAt(Instant v)         { this.createdAt         = v; return this; }
    public Reconstructor isDeleted(boolean v)         { this.isDeleted         = v; return this; }

    // ✅ Gọi đúng constructor User(Reconstructor r)
    public User build() { return new User(this); }
  }
}