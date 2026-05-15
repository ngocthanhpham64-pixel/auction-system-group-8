package vn.edu.vnu.uet.group8.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class AuctionSummaryDTO {
    private int id;
    private String title;
    private BigDecimal currentPrice;
    private String status; // ACTIVE, ENDED_NO_BID, CANCELLED
    private Instant endTime;
    public AuctionSummaryDTO() {}

    public AuctionSummaryDTO(int id, String title, BigDecimal currentPrice, String status, Instant endTime) {
        this.id = id;
        this.title = title;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}
