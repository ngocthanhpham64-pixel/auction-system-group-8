package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

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

    @SerializedName("sold_auctions")
    private int soldAuctions;

    @SerializedName("cancelled_auctions")
    private int cancelledAuctions;

    @SerializedName("upcoming_auctions")
    private int upcomingAuctions;

    @SerializedName("ended_no_bid_auctions")
    private int endedNoBidAuctions;

    /** No-arg constructor cho Gson. */
    public AdminStatsDTO() {}

    public AdminStatsDTO(int activeAuctions, int totalUsers, BigDecimal totalRevenue, int totalBids) {
        this.activeAuctions = activeAuctions;
        this.totalUsers = totalUsers;
        this.totalRevenue = totalRevenue;
        this.totalBids = totalBids;
    }
    public AdminStatsDTO(int activeAuctions, int totalUsers, BigDecimal totalRevenue, int totalBids,
                         int soldAuctions, int cancelledAuctions, int upcomingAuctions, int endedNoBidAuctions) {
        this(activeAuctions, totalUsers, totalRevenue, totalBids);
        this.soldAuctions       = soldAuctions;
        this.cancelledAuctions  = cancelledAuctions;
        this.upcomingAuctions   = upcomingAuctions;
        this.endedNoBidAuctions = endedNoBidAuctions;
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

    public int getSoldAuctions()       { return soldAuctions; }
    public int getCancelledAuctions()  { return cancelledAuctions; }
    public int getUpcomingAuctions()   { return upcomingAuctions; }
    public int getEndedNoBidAuctions() { return endedNoBidAuctions; }

    /** Null-safe: nếu server trả null thì trả về ZERO. */
    public BigDecimal getTotalRevenue() {
        return totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    // ===== Setters =====

    public void setActiveAuctions(int activeAuctions) { this.activeAuctions = activeAuctions; }
    public void setTotalUsers(int totalUsers)         { this.totalUsers = totalUsers; }
    public void setTotalRevenue(BigDecimal revenue)   { this.totalRevenue = revenue; }
    public void setTotalBids(int totalBids)           { this.totalBids = totalBids; }

    public void setSoldAuctions(int v)       { this.soldAuctions = v; }
    public void setCancelledAuctions(int v)  { this.cancelledAuctions = v; }
    public void setUpcomingAuctions(int v)   { this.upcomingAuctions = v; }
    public void setEndedNoBidAuctions(int v) { this.endedNoBidAuctions = v; }
    // ===== Object overrides =====

    @Override
    public String toString() {
        return "AdminStatsDTO{" +
                "activeAuctions=" + activeAuctions +
                ", totalUsers=" + totalUsers +
                ", totalRevenue=" + getTotalRevenue() +
                ", totalBids=" + totalBids +
                ", soldAuctions="       + soldAuctions +
                ", cancelledAuctions="  + cancelledAuctions +
                ", upcomingAuctions="   + upcomingAuctions +
                ", endedNoBidAuctions=" + endedNoBidAuctions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminStatsDTO that)) return false;
        return activeAuctions == that.activeAuctions
                && totalUsers == that.totalUsers
                && totalBids == that.totalBids
                && soldAuctions      == that.soldAuctions
                && cancelledAuctions == that.cancelledAuctions
                && upcomingAuctions  == that.upcomingAuctions
                && endedNoBidAuctions== that.endedNoBidAuctions
                && Objects.equals(getTotalRevenue(), that.getTotalRevenue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(activeAuctions, totalUsers, getTotalRevenue(), totalBids,
                soldAuctions, cancelledAuctions, upcomingAuctions, endedNoBidAuctions);
    }
}