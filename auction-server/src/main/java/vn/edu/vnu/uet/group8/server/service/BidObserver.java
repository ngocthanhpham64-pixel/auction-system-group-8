package vn.edu.vnu.uet.group8.server.service;

public interface BidObserver {
  // Thông báo cho ClientHandler biết có giá mới để nó đẩy qua Socket xuống Client
  void onBidUpdated(String auctionId, double newPrice, String winner);
}