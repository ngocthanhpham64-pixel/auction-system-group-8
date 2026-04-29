package vn.edu.vnu.uet.group8.common.dto;

import java.math.BigDecimal;

/**
 * DTO cho yêu cầu đặt giá tự động (proxy bidding).
 * Client gửi lên server để thiết lập tự động đặt giá cho một item.
 */
public final class AutoBidRequest {
    private final int itemId;
    private final int userId;
    private final BigDecimal maxPrice;
    private final BigDecimal stepAmount; // bước tăng tối thiểu (có thể null nếu server tự lấy)

    // No-arg constructor CHO GSON (private, nhưng Gson vẫn dùng được)
    private AutoBidRequest() {
        this.itemId = 0;
        this.userId = 0;
        this.maxPrice = null;
        this.stepAmount = null;
    }

    private AutoBidRequest(Builder builder) {
        this.itemId = builder.itemId;
        this.userId = builder.userId;
        this.maxPrice = builder.maxPrice;
        this.stepAmount = builder.stepAmount;
    }

    // Getters
    public int getItemId() { return itemId; }
    public int getUserId() { return userId; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public BigDecimal getStepAmount() { return stepAmount; }

    @Override
    public String toString() {
        return "AutoBidRequest{" +
                "itemId=" + itemId +
                ", userId=" + userId +
                ", maxPrice=" + maxPrice +
                ", stepAmount=" + stepAmount +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int itemId;
        private int userId;
        private BigDecimal maxPrice;
        private BigDecimal stepAmount; // optional

        private Builder() {}

        public Builder itemId(int itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder userId(int userId) {
            this.userId = userId;
            return this;
        }

        public Builder maxPrice(BigDecimal maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public Builder stepAmount(BigDecimal stepAmount) {
            this.stepAmount = stepAmount;
            return this;
        }

        public AutoBidRequest build() {
            // VALIDATION BẮT BUỘC – theo đúng guidelines
            if (itemId <= 0) {
                throw new IllegalArgumentException("itemId phải lớn hơn 0");
            }
            if (userId <= 0) {
                throw new IllegalArgumentException("userId phải lớn hơn 0");
            }
            if (maxPrice == null) {
                throw new IllegalArgumentException("maxPrice không được null");
            }
            if (maxPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("maxPrice phải lớn hơn 0");
            }
            if (stepAmount != null && stepAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("stepAmount nếu có phải > 0");
            }
            return new AutoBidRequest(this);
        }
    }
}