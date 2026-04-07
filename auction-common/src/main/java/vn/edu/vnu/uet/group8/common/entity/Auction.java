package vn.edu.vnu.uet.group8.common.entity;

import vn.edu.vnu.uet.group8.common.enums.AuctionStatus;
import java.io.Serializable;

// Class này dùng để gửi qua lại giữa Client - Server nên cần implements Serializable
public class Auction implements Serializable {
  private static final long serialVersionUID = 1L;

  private String id;
  private double currentPrice;
  private String highestBidder;
  private AuctionStatus status;

  public Auction(String id, double startPrice){
    this.id = id;
    this.currentPrice = startPrice;
    this.status = AuctionStatus.OPEN;
    this.highestBidder = null;
  }
  public String getId() {
    return id;
  }
  public double getCurrentPrice() {
    return currentPrice;
  }
  public void setCurrentPrice(double currentPrice) {
    this.currentPrice = currentPrice;
  }
  public String getHighestBidder() {
    return highestBidder;
  }
  public void setHighestBidder(String highestBidder) {
    this.highestBidder = highestBidder;
  }
  public AuctionStatus getStatus() {
    return status;
  }
  public void setStatus(AuctionStatus status) {
    this.status = status;
  }
}