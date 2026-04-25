package vn.edu.vnu.uet.group8.common.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import vn.edu.vnu.uet.group8.common.enums.UserRole;
import vn.edu.vnu.uet.group8.common.enums.UserStatus;
// import vn.edu.vnu.uet.group8.common.exception.InsufficientBalanceException;

public class UserMember extends User {
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
    this.avatarUrl = b.avatarUrl;
    this.totalBidsPlaced = b.totalBidsPlaced;
    this.totalItemsSold = b.totalItemsSold;
    this.sellerRating = b.sellerRating;
    this.balance = b.balance;
  }

  private UserMember(Reconstructor r) {
    super(r.id, r.createdAt, r.isDeleted, r.username, r.email, r.fullName,
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


  // ════════════════════════════════════════════════════
  // CONSTRUCTOR
  // ════════════════════════════════════════════════════

  public static class Reconstructor {
    // Dùng Integer (wrapper) thay vì int primitive
    // để phân biệt "chưa set" (null) với "set = 0"
    private Integer id;
    private Instant createdAt;
    private Boolean isDeleted;
    private String username;
    private String fullName;        // nullable — user chưa điền
    private String encryptedPassword;
    private String email;
    private UserStatus status;
    private Set<UserRole> roles;
    private Instant lastLogin;      // nullable — chưa login lần nào
    private BigDecimal balance;
    private String phone;           
    private String address;         
    private String avatarUrl;       // nullable
    private int totalBidsPlaced;    // nullable
    private int totalItemsSold;     // nullable
    private BigDecimal sellerRating;// nullable — chưa có đánh giá

    // Các method set không validate — tin tưởng DB
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
      this.lastLogin = v; return this;   // null = chưa login
    }
    public Reconstructor balance(BigDecimal v) {
      this.balance = v; return this;
    }
    public Reconstructor fullName(String v) {
      this.fullName = v; return this;
    }
    public Reconstructor phone(String v) {
      this.phone = v; return this;
    }
    public Reconstructor sellerRating(BigDecimal v) {
      this.sellerRating = v; return this;
    }
    public Reconstructor address(String v) {
      this.address = v; return this;
    }
    public Reconstructor avatarUrl(String v) {
      this.avatarUrl = v; return this;
    }
    public Reconstructor totalBidsPlaced(int v) {
      this.totalBidsPlaced = v; return this;
    }
    public Reconstructor totalItemsSold(int v) {
      this.totalItemsSold = v; return this;
    }

    /**
     * Chỉ kiểm tra những field KHÔNG BAO GIỜ được null từ DB.
     * Không validate logic nghiệp vụ — đó là việc của Builder.
     */
    public UserMember build() {
      // Chỉ check các NOT NULL column trong DB
      requireNonNull(id,                "id");
      requireNonNull(createdAt,         "createdAt");
      requireNonNull(isDeleted,         "isDeleted");
      requireNonNull(username,          "username");
      requireNonNull(email,             "email");
      requireNonNull(encryptedPassword, "encryptedPassword");
      requireNonNull(status,            "status");
      requireNonNull(roles,             "roles");
      requireNonNull(balance,           "balance");
      requireNonNull(phone,             "phone");
      // lastLogin, fullName, sellerRating, avatarUrl,
      // totalBidsPlaced, totalItemsSold → nullable, không check

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
    private String phone    = "";
    private String address  = "";
    private String avatarUrl= "";
    private BigDecimal balance = BigDecimal.ZERO;
    private int totalBidsPlaced = 0;
    private int totalItemsSold = 0;
    private BigDecimal sellerRating = null;

    public Builder(String username, String email, String encryptedPassword) {
      super(username, email, encryptedPassword);
    }

    public Builder phone(String phone) {
      if (phone != null && !phone.isBlank()) {
        throw new IllegalArgumentException(
          "Phone không được trống");
      }
      this.phone = phone;
      return this;
    }

    public Builder address(String address) {
      if (address != null && !address.isBlank()) {
        throw new IllegalArgumentException(
          "Address không được trống");
      }
      this.address = address;
      return this;
    }

    public Builder avatarUrl(String avatarUrl) {
      if (avatarUrl != null && !avatarUrl.isBlank()) {
        this.avatarUrl = "";
      }
      this.avatarUrl = avatarUrl;
      return this;
    }

    public Builder balance() {
      this.balance = BigDecimal.ZERO;
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
      return new UserMember(this);
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

  public int gettotalItemsSold() {
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
    if (address == null || address.isBlank()) {
      throw new IllegalArgumentException(
        "Address không được trống");
    }
    this.address = address != null ? address.trim() : "";
  }

  public void setPhone(String phone) {
    if (phone == null || phone.isBlank()) {
      throw new IllegalArgumentException(
        "Số điện thoại không được để trống");
    }
    this.phone = phone != null ? phone.trim() : "";
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }


  // /** Gọi sau khi tổng hợp rating mới từ bảng ratings */
  // public void updateSellerRating(BigDecimal rating) {
  //   if (rating != null
  //       && (rating.compareTo(BigDecimal.ZERO) < 0
  //       || rating.compareTo(new BigDecimal("5.0")) > 0))
  //         throw new IllegalArgumentException(
  //           "Rating phải trong khoảng 0.0 - 5.0");
  //     this.sellerRating = rating;
  // }

  // ════════════════════════════════════════════════════
  // HELPER CÁC HÀM NGHIỆP VỤ
  // ════════════════════════════════════════════════════
  // public void deposit(BigDecimal amount) {
  //   if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
  //     throw new IllegalArgumentException(
  //       "Số tiền phải lớn hơn 0");
  //   }
  //   this.balance = this.balance.add(amount);
  //   // Có thể ghi log ở đây
  //   // Log.info("Member " + getUsername() + " đã nạp " + amount);
  // }

  // public void withdraw(BigDecimal amount) {
  //   if (amount == null || amount.compareTo(this.balance) > 0) {
  //     throw new InsufficientBalanceException("Số dư không đủ để thực hiện giao dịch");
  //   }
  //   this.balance = this.balance.subtract(amount);
  //   // Có thể ghi log ở đây
  //   // Log.info("Member " + getUsername() + " đã rút " + amount);
  // }

  // ════════════════════════════════════════════════════
  // OVERRIDE từ User
  // ════════════════════════════════════════════════════
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
               "id='"        + getId()      + '\'' +
               ", username='" + getUsername() + '\'' +
               ", fullName='" + getFullName()   + '\'' +
               ", status="   + getStatus() +
               ", roles="    + getRoles()  +
               '}';
    }
}