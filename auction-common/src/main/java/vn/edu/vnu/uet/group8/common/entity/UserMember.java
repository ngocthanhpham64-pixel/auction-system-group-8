package vn.edu.vnu.uet.group8.common.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
// import vn.edu.vnu.uet.group8.common.exception.InsufficientBalanceException;

public final class UserMember extends User {
  private String phone;
  private String address;
  private String avatarUrl;
  private BigDecimal balance;

  private int totalBidsPlaced;
  private int totalItemsSold;
  private BigDecimal sellerRating;

  private UserMember(Builder b) {
    super(b);
    addRole(UserRole.BIDDER);
    this.phone = b.phone;
    this.address = b.address;
    this.avatarUrl = b.avatarUrl == null ? "" : b.avatarUrl;
    this.totalBidsPlaced = b.totalBidsPlaced;
    this.totalItemsSold = b.totalItemsSold;
    this.sellerRating = b.sellerRating;
    this.balance = b.balance;
  }

  private UserMember(Reconstructor r) {
    super(r.id, r.createdAt, r.isDeleted, r.username, r.email, r.fullname,
        r.encryptedPassword, r.status, r.roles, r.lastLogin);
    this.phone = r.phone;
    this.address = r.address;
    this.avatarUrl = r.avatarUrl;
    this.totalBidsPlaced = r.totalBidsPlaced;
    this.totalItemsSold = r.totalItemsSold;
    this.sellerRating = r.sellerRating;
    this.balance = r.balance;
  }

  public static Reconstructor reconstructor() {
    return new Reconstructor();
  }

  public static Builder builder(String username, String email, String encryptedPassword) {
    return new Builder(username, email, encryptedPassword);
  }

  // ════════════════════════════════════════════════════
  // RECONSTRUCTOR
  // ════════════════════════════════════════════════════

  public static class Reconstructor {
    // Dùng Integer (wrapper) thay vì int primitive
    // để phân biệt "chưa set" (null) với "set = 0"
    private Integer id;
    private Instant createdAt;
    private Boolean isDeleted;
    private String username;
    private String fullname; // nullable
    private String encryptedPassword;
    private String email;
    private UserStatus status;
    private Set<UserRole> roles;
    private Instant lastLogin; // nullable — chưa login lần nào
    private BigDecimal balance;
    private String phone;
    private String address; // nullable
    private String avatarUrl; // nullable
    private int totalBidsPlaced; // nullable
    private int totalItemsSold; // nullable
    private BigDecimal sellerRating;// nullable — chưa có đánh giá

    // Các method set không validate — tin tưởng DB
    public Reconstructor id(int id) {
      this.id = id;
      return this;
    }

    public Reconstructor createdAt(Instant v) {
      this.createdAt = v;
      return this;
    }

    public Reconstructor isDeleted(boolean v) {
      this.isDeleted = v;
      return this;
    }

    public Reconstructor username(String v) {
      this.username = v;
      return this;
    }

    public Reconstructor email(String v) {
      this.email = v;
      return this;
    }

    public Reconstructor encryptedPassword(String v) {
      this.encryptedPassword = v;
      return this;
    }

    public Reconstructor status(UserStatus v) {
      this.status = v;
      return this;
    }

    public Reconstructor roles(Set<UserRole> v) {
      this.roles = v;
      return this;
    }

    public Reconstructor lastLogin(Instant v) {
      this.lastLogin = v;
      return this; // null = chưa login
    }

    public Reconstructor balance(BigDecimal v) {
      this.balance = v;
      return this;
    }

    public Reconstructor fullname(String v) {
      this.fullname = v;
      return this;
    }

    public Reconstructor phone(String v) {
      this.phone = v;
      return this;
    }

    public Reconstructor sellerRating(BigDecimal v) {
      this.sellerRating = v;
      return this;
    }

    public Reconstructor address(String v) {
      this.address = v;
      return this;
    }

    public Reconstructor avatarUrl(String v) {
      this.avatarUrl = v;
      return this;
    }

    public Reconstructor totalBidsPlaced(int v) {
      this.totalBidsPlaced = v;
      return this;
    }

    public Reconstructor totalItemsSold(int v) {
      this.totalItemsSold = v;
      return this;
    }

    /**
     * Chỉ kiểm tra những field KHÔNG BAO GIỜ được null từ DB.
     * Không validate logic nghiệp vụ — đó là việc của Builder.
     */
    public UserMember build() {
      // Chỉ check các NOT NULL column trong DB
      requireNonNull(id, "id");
      requireNonNull(createdAt, "createdAt");
      requireNonNull(isDeleted, "isDeleted");
      requireNonNull(username, "username");
      requireNonNull(email, "email");
      requireNonNull(encryptedPassword, "encryptedPassword");
      requireNonNull(status, "status");
      requireNonNull(roles, "roles");
      requireNonNull(balance, "balance");
      requireNonNull(phone, "phone");
      // lastLogin, sellerRating, avatarUrl, fullname,
      // totalBidsPlaced, totalItemsSold, address → nullable, không check

      return new UserMember(this); // constructor private nhận Reconstructor
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
    // Optional
    private String phone = "";
    private String address = "";
    private String avatarUrl = "";
    private BigDecimal balance = BigDecimal.ZERO;
    private int totalBidsPlaced = 0;
    private int totalItemsSold = 0;
    private BigDecimal sellerRating = null;

    public Builder(String username, String email, String encryptedPassword) {
      super(username, email, encryptedPassword);
    }

    public Builder fullname(String fullname) {
      super.fullname(fullname);
      return this;
    }

    public Builder phone(String phone) {
      checkRequiredString(phone, "Phone không được trống");
      this.phone = phone;
      return this;
    }

    public Builder address(String address) {
      this.address = address;
      return this;
    }

    public Builder avatarUrl(String avatarUrl) {
      this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
      return this;
    }

    public Builder balance() {
      this.balance = BigDecimal.ZERO;
      return this;
    }

    public Builder balance(BigDecimal balance) {
      if (balance != null && balance.compareTo(BigDecimal.ZERO) >= 0) {
        this.balance = balance;
      }
      return this;
    }

    public Builder totalBidsPlaced() {
      this.totalBidsPlaced = 0;
      return this;
    }

    public Builder totalItemsSold() {
      this.totalItemsSold = 0;
      return this;
    }

    public Builder sellerRating() {
      this.sellerRating = null;
      return this;
    }

    @Override
    public UserMember build() {
      requireNonNull(phone, "phone");
      return new UserMember(this);
    }

    private void checkRequiredString(String str, String message) {
      if (str == null || str.isBlank()) {
        throw new IllegalArgumentException(message);
      }
    }

    private void requireNonNull(Object value, String fieldName) {
      if (value == null)
        throw new IllegalStateException(
            "User thiếu field bắt buộc: [" + fieldName + "]. "
                + "Hãy kiểm tra lại");
    }
  }

  // ════════════════════════════════════════════════════
  // GETTERS
  // ════════════════════════════════════════════════════
  public String getPhone() {
    return phone;
  }

  public String getAddress() {
    return address;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public int getTotalBidsPlaced() {
    return totalBidsPlaced;
  }

  public int getTotalItemsSold() {
    return totalItemsSold;
  }

  public BigDecimal getSellerRating() {
    return sellerRating;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  // ════════════════════════════════════════════════════
  // SETTERS
  // ════════════════════════════════════════════════════
  public void setAddress(String address) {
    this.address = address != null ? address.trim() : "";
  }

  public void setPhone(String phone) {
    checkRequiredString(phone, "Số điện thoại không được để trống");
    this.phone = phone != null ? phone.trim() : "";
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  // /** Gọi sau khi tổng hợp rating mới từ bảng ratings */
  // public void updateSellerRating(BigDecimal rating) {
  // if (rating != null
  // && (rating.compareTo(BigDecimal.ZERO) < 0
  // || rating.compareTo(new BigDecimal("5.0")) > 0))
  // throw new IllegalArgumentException(
  // "Rating phải trong khoảng 0.0 - 5.0");
  // this.sellerRating = rating;
  // }

  // ════════════════════════════════════════════════════
  // Các hàm HELPER
  // ════════════════════════════════════════════════════
  private void checkRequiredString(String str, String message) {
    if (str == null || str.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }

  // ════════════════════════════════════════════════════
  // OVERRIDE từ User
  // ════════════════════════════════════════════════════
  @Override
  public void setFullname(String fullname) {
    checkRequiredString(fullname, "Họ tên không được trống");
    super.setFullname(fullname);
  }

  @Override
  public boolean isAdmin() {
    return false;
  }

  @Override
  public String getDisplayRole() {
    if (hasRole(UserRole.SELLER) && hasRole(UserRole.BIDDER))
      return "Người mua & Người bán";
    if (hasRole(UserRole.SELLER))
      return "Người bán";
    return "Người mua";
  }

  @Override
  public String toString() {
    return "MemberUser{" +
        "id='" + getId() + '\'' +
        ", username='" + getUsername() + '\'' +
        ", fullName='" + getFullname() + '\'' +
        ", status=" + getStatus() +
        ", roles=" + getRoles() +
        '}';
  }
}