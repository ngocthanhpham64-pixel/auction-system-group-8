package vn.edu.vnu.uet.group8.common.dto;

import java.math.BigDecimal;

public class AdminStatsDTO {
    private int activeAuctions;
    private int totalUsers;
    private BigDecimal totalRevenue;
    private int totalBids;

    public AdminStatsDTO(){}

    public AdminStatsDTO( int activeAuctions, int totalUsers, BigDecimal totalRevenue, int totalBids){
        this.activeAuctions = activeAuctions;
        this.totalUsers = totalUsers;
        this.totalRevenue = totalRevenue;
        this.totalBids = totalBids;
    }
    public int getActiveAuctions(){ return activeAuctions;}
    public void setActiveAuctions(int activeAuctions){ this.activeAuctions = activeAuctions;}
    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public int getTotalBids() { return totalBids; }
    public void setTotalBids(int totalBids) { this.totalBids = totalBids; }
}
