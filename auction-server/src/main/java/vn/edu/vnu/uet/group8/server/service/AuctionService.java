package vn.edu.vnu.uet.group8.server.service;

import vn.edu.vnu.uet.group8.common.entity.Auction;
import vn.edu.vnu.uet.group8.common.enums.AuctionStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {
  // Quản lý danh sách phiên đấu giá
  private final Map <String, Auction> auctions = new ConcurrentHashMap<>();

  private static volatile AuctionService instance;

  public static AuctionService getInstance() {
    if (instance == null) {
      synchronized (AuctionService.class) {
        if (instance == null) instance = new AuctionService();
      }
    }
    return instance;
  }

  // Quản lý LOCK riêng cho TỪNG phiên đấu giá (Fine-grained locking)
  private final Map<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

  // Danh sách các luồng mạng (ClientHandler) đang kết nối để nhận thông báo
  // Dùng CopyOnWriteArrayList để tránh ConcurrentModificationException khi đa luồng
  private final List<BidObserver> observers = new CopyOnWriteArrayList<>();

  // Nhiệm vụ 1: Observer Pattern
  public void addObserver(BidObserver observer) {
    observers.add(observer);
  }

  public void removeObserver(BidObserver observer) {
    observers.remove(observer);
  }

  private void notifyObservers(String auctionId, double price, String winner) {
    for (BidObserver o : observers) {
      o.onBidUpdated(auctionId, price, winner);
    }
  }
  // NHIỆM VỤ 2 & 3: LOGIC & CHUYỂN TRẠNG THÁI
  public void createAuction(String id, double startPrice) {
    auctions.put(id, new Auction(id, startPrice));
    auctionLocks.put(id, new ReentrantLock());
  }

  public void startAuction(String id) {
    ReentrantLock lock = auctionLocks.get(id);
    if (lock == null) return;

    lock.lock();
    try {
      Auction auction = auctions.get(id);
      if (auction.getStatus() == AuctionStatus.OPEN) {
        auction.setStatus(AuctionStatus.RUNNING);
      }
    } finally {
      lock.unlock();
    }
  }
  // NHIỆM VỤ 4 & 5: ĐẶT GIÁ ĐỒNG THỜI CÓ LOCK
  public boolean placeBid(String auctionId, double price, String username) {
    Auction auction = auctions.get(auctionId);
    ReentrantLock lock = auctionLocks.get(auctionId);

    if (auction == null || lock == null) return false;

    boolean isSuccess = false;

    // 1. CHẶN CÁC LUỒNG KHÁC (Synchronized operation)
    lock.lock();
    try {
      // 2. Kiểm tra trạng thái hợp lệ
      if (auction.getStatus() != AuctionStatus.RUNNING) {
        return false;
      }

      // 3. Kiểm tra logic giá
      if (price <= auction.getCurrentPrice()) {
        return false;
      }
      if (username.equals(auction.getHighestBidder())) {
        return false; // Tránh tự bid đè lên chính mình
      }

      // 4. Thực hiện cập nhật an toàn
      auction.setCurrentPrice(price);
      auction.setHighestBidder(username);
      isSuccess = true;

    } finally {
      // 5. GIẢI PHÓNG LOCK NGAY LẬP TỨC
      lock.unlock();
    }

    // 6. CHỈ NOTIFY KHI ĐÃ UNLOCK (Tránh Deadlock và Nghẽn cổ chai)
    if (isSuccess) {
      notifyObservers(auctionId, price, username);
    }

    return isSuccess;
  }

  public Auction getAuction(String auctionId) {
    return auctions.get(auctionId);   // trả null nếu không tìm thấy
  }
}