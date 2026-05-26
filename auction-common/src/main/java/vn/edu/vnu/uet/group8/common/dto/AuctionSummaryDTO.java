package vn.edu.vnu.uet.group8.common.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * DTO mô tả gọn 1 phiên đấu giá để hiển thị trong danh sách quản lý admin.
 *
 * <h3>Đặc điểm thiết kế:</h3>
 * <ul>
 *   <li><b>Status constants</b> thay vì String thô - tránh typo, dễ refactor</li>
 *   <li><b>Helper isActive()/isEnded()/isCancelled()</b> - logic gom 1 chỗ</li>
 *   <li><b>Null-safe getter</b> cho BigDecimal (ZERO) và String (rỗng)</li>
 *   <li><b>@SerializedName snake_case</b> - thống nhất với JSON convention BE</li>
 *   <li><b>toString/equals/hashCode</b> - dễ log + test</li>
 * </ul>
 *
 * <h3>Các trạng thái phiên đấu giá:</h3>
 * <ul>
 *   <li>{@link #STATUS_ACTIVE} - đang diễn ra (chưa hết hạn, chưa bị hủy)</li>
 *   <li>{@link #STATUS_SOLD} - đã bán thành công (có người thắng)</li>
 *   <li>{@link #STATUS_ENDED_NO_BID} - hết hạn nhưng không có ai bid</li>
 *   <li>{@link #STATUS_CANCELLED} - bị admin hủy</li>
 * </ul>
 */
public class AuctionSummaryDTO {

    // ===== Status constants =====
    public static final String STATUS_ACTIVE       = "ACTIVE";
    public static final String STATUS_SOLD         = "SOLD";
    public static final String STATUS_ENDED_NO_BID = "ENDED_NO_BID";
    public static final String STATUS_CANCELLED    = "CANCELLED";

    private int id;
    private String title;

    @SerializedName("current_price")
    private BigDecimal currentPrice;

    private String status;

    @SerializedName("end_time")
    private Instant endTime;

    /** No-arg constructor cho Gson. */
    public AuctionSummaryDTO() {}

    public AuctionSummaryDTO(int id, String title, BigDecimal currentPrice, String status, Instant endTime) {
        this.id = id;
        this.title = title;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
    }

    // ===== Getters (null-safe) =====

    public int getId() { return id; }

    public String getTitle() {
        return title != null ? title : "";
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice != null ? currentPrice : BigDecimal.ZERO;
    }

    public String getStatus() {
        return status != null ? status : "";
    }

    public Instant getEndTime() { return endTime; }

    // ===== Setters =====

    public void setId(int id)                          { this.id = id; }
    public void setTitle(String title)                 { this.title = title; }
    public void setCurrentPrice(BigDecimal price)      { this.currentPrice = price; }
    public void setStatus(String status)               { this.status = status; }
    public void setEndTime(Instant endTime)            { this.endTime = endTime; }

    // ===== Status helpers =====

    /** Phiên có thể hủy được không (chỉ ACTIVE mới hủy được). */
    public boolean isActive()    { return STATUS_ACTIVE.equalsIgnoreCase(status); }
    public boolean isSold()      { return STATUS_SOLD.equalsIgnoreCase(status); }
    public boolean isCancelled() { return STATUS_CANCELLED.equalsIgnoreCase(status); }

    /** Đã kết thúc bằng bất kỳ cách nào (SOLD / ENDED_NO_BID / CANCELLED). */
    public boolean isEnded() {
        return STATUS_SOLD.equalsIgnoreCase(status)
                || STATUS_ENDED_NO_BID.equalsIgnoreCase(status)
                || STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    /** Phiên đã hết thời gian (theo endTime) nhưng có thể status chưa cập nhật. */
    public boolean isExpired() {
        return endTime != null && endTime.isBefore(Instant.now());
    }

    // ===== Object overrides =====

    @Override
    public String toString() {
        return "AuctionSummaryDTO{" +
                "id=" + id +
                ", title='" + getTitle() + '\'' +
                ", currentPrice=" + getCurrentPrice() +
                ", status='" + getStatus() + '\'' +
                ", endTime=" + endTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuctionSummaryDTO that)) return false;
        return id == that.id;  // id là khóa chính
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}