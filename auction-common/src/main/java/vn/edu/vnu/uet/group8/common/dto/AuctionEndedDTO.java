package vn.edu.vnu.uet.group8.common.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * DTO server push khi phiên đấu giá kết thúc.
 *
 * <p>Gửi qua broadcast {@code EventType.AUCTION_ENDED}.
 *
 * <h3>Hai trường hợp kết thúc:</h3>
 * <ul>
 *   <li><b>SOLD</b> - {@code sold = true}, có {@code winnerUsername}, {@code finalPrice} là giá thắng</li>
 *   <li><b>NO_BID</b> - {@code sold = false}, {@code winnerUsername = null}, {@code finalPrice}
 *       là giá khởi điểm (không ai đặt)</li>
 * </ul>
 *
 * <h3>Cải thiện so với bản cũ:</h3>
 * <ul>
 *   <li><b>FIX: no-arg constructor public</b> (cũ là private → Gson không deserialize được)</li>
 *   <li><b>@SerializedName snake_case</b> - khớp convention JSON BE</li>
 *   <li><b>Null-safe getters</b> - BigDecimal trả ZERO, String trả "" nếu null</li>
 *   <li><b>Helper isNoBid()</b> - đối ngẫu với isSold()</li>
 *   <li><b>toString/equals/hashCode</b> - dễ log + test</li>
 * </ul>
 */
public final class AuctionEndedDTO {

    @SerializedName("item_id")
    private int itemId;

    /** true = có người thắng, false = không có ai bid */
    private boolean sold;

    @SerializedName("winner_username")
    private String winnerUsername;  // null nếu NO_BID

    @SerializedName("final_price")
    private BigDecimal finalPrice;

    @SerializedName("ended_at")
    private Instant endedAt;

    /**
     * No-arg constructor cho Gson deserialize.
     * <p><b>FIX:</b> Bản cũ để private — Gson không tạo được instance → parse fail.
     */
    public AuctionEndedDTO() {}

    public AuctionEndedDTO(int itemId, boolean sold, String winnerUsername,
                           BigDecimal finalPrice, Instant endedAt) {
        this.itemId = itemId;
        this.sold = sold;
        this.winnerUsername = winnerUsername;
        this.finalPrice = finalPrice;
        this.endedAt = endedAt;
    }

    // ===== Getters (null-safe cho String/BigDecimal) =====

    public int getItemId() { return itemId; }

    public boolean isSold() { return sold; }

    /** Đối ngẫu với isSold — dễ đọc trong điều kiện. */
    public boolean isNoBid() { return !sold; }

    public String getWinnerUsername() {
        return winnerUsername != null ? winnerUsername : "";
    }

    public BigDecimal getFinalPrice() {
        return finalPrice != null ? finalPrice : BigDecimal.ZERO;
    }

    public Instant getEndedAt() { return endedAt; }

    // ===== Object overrides =====

    @Override
    public String toString() {
        return "AuctionEndedDTO{" +
                "itemId=" + itemId +
                ", sold=" + sold +
                ", winnerUsername='" + getWinnerUsername() + '\'' +
                ", finalPrice=" + getFinalPrice() +
                ", endedAt=" + endedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuctionEndedDTO that)) return false;
        return itemId == that.itemId
                && sold == that.sold
                && Objects.equals(getWinnerUsername(), that.getWinnerUsername())
                && Objects.equals(getFinalPrice(), that.getFinalPrice())
                && Objects.equals(endedAt, that.endedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, sold, getWinnerUsername(), getFinalPrice(), endedAt);
    }
}