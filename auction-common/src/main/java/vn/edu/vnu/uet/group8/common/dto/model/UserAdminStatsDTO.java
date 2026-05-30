package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * DTO truyền số liệu thống kê chi tiết của một người dùng từ server xuống admin client.
 *
 * <h3>Đặc điểm thiết kế:</h3>
 * <ul>
 * <li><b>POJO Gson-compatible</b> - vẫn có no-arg constructor và setter để Gson deserialize</li>
 * <li><b>Null-safe getter</b> cho BigDecimal (trả về ZERO nếu null) - tránh NPE phía UI</li>
 * <li><b>@SerializedName</b> - field name JSON cố định, không phụ thuộc field name Java</li>
 * <li><b>toString/equals/hashCode</b> - dễ log, dễ test</li>
 * <li><b>Factory empty()</b> - placeholder khi load chưa xong</li>
 * </ul>
 */
public class UserAdminStatsDTO {

    @SerializedName("user_id")
    private int userId;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("display_role")
    private String displayRole;

    @SerializedName("last_login")
    private Instant lastLogin;

    @SerializedName("total_bids_placed")
    private int totalBidsPlaced;

    @SerializedName("total_items_sold")
    private int totalItemsSold;

    @SerializedName("auctions_sold")
    private int auctionsSold;

    @SerializedName("total_earned")
    private BigDecimal totalEarned;

    @SerializedName("seller_rating")
    private double sellerRating;

    @SerializedName("total_spent")
    private BigDecimal totalSpent;

    /** No-arg constructor cho Gson. */
    public UserAdminStatsDTO() {}

    public UserAdminStatsDTO(int userId, String username, String email, String displayRole, Instant lastLogin,
                             int totalBidsPlaced, int totalItemsSold, int auctionsSold,
                             BigDecimal totalEarned, double sellerRating, BigDecimal totalSpent) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.displayRole = displayRole;
        this.lastLogin = lastLogin;
        this.totalBidsPlaced = totalBidsPlaced;
        this.totalItemsSold = totalItemsSold;
        this.auctionsSold = auctionsSold;
        this.totalEarned = totalEarned;
        this.sellerRating = sellerRating;
        this.totalSpent = totalSpent;
    }

    /**
     * Tạo instance rỗng làm placeholder khi UI cần hiển thị trước
     * lúc data thật về (tránh null check khắp nơi).
     */
    public static UserAdminStatsDTO empty() {
        return new UserAdminStatsDTO(0, "", "", "", null, 0, 0, 0, BigDecimal.ZERO, 0.0, BigDecimal.ZERO);
    }

    // ===== Getters =====

    public int getUserId() { return userId; }
    public String getUsername() { return username != null ? username : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public String getDisplayRole() { return displayRole != null ? displayRole : ""; }
    public Instant getLastLogin() { return lastLogin; }
    public int getTotalBidsPlaced() { return totalBidsPlaced; }
    public int getTotalItemsSold() { return totalItemsSold; }
    public int getAuctionsSold() { return auctionsSold; }
    public double getSellerRating() { return sellerRating; }

    /** Null-safe: nếu server trả null thì trả về ZERO. */
    public BigDecimal getTotalEarned() { return totalEarned != null ? totalEarned : BigDecimal.ZERO; }
    public BigDecimal getTotalSpent() { return totalSpent != null ? totalSpent : BigDecimal.ZERO; }

    // ===== Setters =====

    public void setUserId(int userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setDisplayRole(String displayRole) { this.displayRole = displayRole; }
    public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; }
    public void setTotalBidsPlaced(int totalBidsPlaced) { this.totalBidsPlaced = totalBidsPlaced; }
    public void setTotalItemsSold(int totalItemsSold) { this.totalItemsSold = totalItemsSold; }
    public void setAuctionsSold(int auctionsSold) { this.auctionsSold = auctionsSold; }
    public void setTotalEarned(BigDecimal totalEarned) { this.totalEarned = totalEarned; }
    public void setSellerRating(double sellerRating) { this.sellerRating = sellerRating; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    // ===== Object overrides =====

    @Override
    public String toString() {
        return "UserAdminStatsDTO{" +
                "userId=" + userId +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", displayRole='" + getDisplayRole() + '\'' +
                ", lastLogin=" + lastLogin +
                ", totalBidsPlaced=" + totalBidsPlaced +
                ", totalItemsSold=" + totalItemsSold +
                ", auctionsSold=" + auctionsSold +
                ", totalEarned=" + getTotalEarned() +
                ", sellerRating=" + sellerRating +
                ", totalSpent=" + getTotalSpent() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAdminStatsDTO that)) return false;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}