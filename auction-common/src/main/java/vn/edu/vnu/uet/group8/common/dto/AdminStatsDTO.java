package vn.edu.vnu.uet.group8.common.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * DTO truyền số liệu Dashboard từ server xuống admin client.
 *
 * <h3>Đặc điểm thiết kế:</h3>
 * <ul>
 *   <li><b>POJO Gson-compatible</b> - vẫn có no-arg constructor và setter để Gson deserialize</li>
 *   <li><b>Null-safe getter</b> cho BigDecimal (trả về ZERO nếu null) - tránh NPE phía UI</li>
 *   <li><b>@SerializedName</b> - field name JSON cố định, không phụ thuộc field name Java</li>
 *   <li><b>toString/equals/hashCode</b> - dễ log, dễ test</li>
 *   <li><b>Factory empty()</b> - placeholder khi load chưa xong</li>
 * </ul>
 *
 * <h3>Ý nghĩa các trường:</h3>
 * <ul>
 *   <li>activeAuctions - số phiên đấu giá đang diễn ra (status = ACTIVE)</li>
 *   <li>totalUsers - tổng số tài khoản đã đăng ký</li>
 *   <li>totalRevenue - tổng doanh thu (VND) từ các phiên SOLD</li>
 *   <li>totalBids - tổng số lượt đặt giá toàn hệ thống</li>
 * </ul>
 */
public class AdminStatsDTO {

    @SerializedName("active_auctions")
    private int activeAuctions;

    @SerializedName("total_users")
    private int totalUsers;

    @SerializedName("total_revenue")
    private BigDecimal totalRevenue;

    @SerializedName("total_bids")
    private int totalBids;

    /** No-arg constructor cho Gson. */
    public AdminStatsDTO() {}

    public AdminStatsDTO(int activeAuctions, int totalUsers, BigDecimal totalRevenue, int totalBids) {
        this.activeAuctions = activeAuctions;
        this.totalUsers = totalUsers;
        this.totalRevenue = totalRevenue;
        this.totalBids = totalBids;
    }

    /**
     * Tạo instance rỗng làm placeholder khi UI cần hiển thị trước
     * lúc data thật về (tránh null check khắp nơi).
     */
    public static AdminStatsDTO empty() {
        return new AdminStatsDTO(0, 0, BigDecimal.ZERO, 0);
    }

    // ===== Getters =====

    public int getActiveAuctions() { return activeAuctions; }
    public int getTotalUsers()     { return totalUsers; }
    public int getTotalBids()      { return totalBids; }

    /** Null-safe: nếu server trả null thì trả về ZERO. */
    public BigDecimal getTotalRevenue() {
        return totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    // ===== Setters =====

    public void setActiveAuctions(int activeAuctions) { this.activeAuctions = activeAuctions; }
    public void setTotalUsers(int totalUsers)         { this.totalUsers = totalUsers; }
    public void setTotalRevenue(BigDecimal revenue)   { this.totalRevenue = revenue; }
    public void setTotalBids(int totalBids)           { this.totalBids = totalBids; }

    // ===== Object overrides =====

    @Override
    public String toString() {
        return "AdminStatsDTO{" +
                "activeAuctions=" + activeAuctions +
                ", totalUsers=" + totalUsers +
                ", totalRevenue=" + getTotalRevenue() +
                ", totalBids=" + totalBids +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminStatsDTO that)) return false;
        return activeAuctions == that.activeAuctions
                && totalUsers == that.totalUsers
                && totalBids == that.totalBids
                && Objects.equals(getTotalRevenue(), that.getTotalRevenue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(activeAuctions, totalUsers, getTotalRevenue(), totalBids);
    }
}