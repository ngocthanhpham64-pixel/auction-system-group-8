package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;

/**
 * Đại diện cho một yêu cầu đặt giá trong hàng đợi ưu tiên (Priority Queue).
 * Dùng chung cho cả đặt giá thủ công và cấu hình tự động (auto-bid).
 */
public record BidCommand(
    int userId,
    BigDecimal maxPrice,
    long timestamp,
    boolean isAuto
) implements Comparable<BidCommand> {

    @Override
    public int compareTo(BidCommand other) {
        // Ưu tiên 1: maxPrice cao hơn thì đứng trước (descending)
        int priceCompare = other.maxPrice().compareTo(this.maxPrice());
        if (priceCompare != 0) {
            return priceCompare;
        }
        // Ưu tiên 2: FCFS - timestamp nhỏ hơn (sớm hơn) thì đứng trước (ascending)
        return Long.compare(this.timestamp(), other.timestamp());
    }
}
