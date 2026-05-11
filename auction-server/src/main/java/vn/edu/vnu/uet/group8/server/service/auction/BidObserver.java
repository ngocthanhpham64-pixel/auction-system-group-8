package vn.edu.vnu.uet.group8.server.service;

import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.PriceUpdateBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;

/**
 * Interface làm cầu nối giữa Service layer và Network layer.
 *
 * Service chỉ gọi interface này — không biết gì về ClientHandler,
 * Socket, hay cách message được gửi đi.
 *
 * Network layer implement interface này theo cách riêng của mình.
 */
public interface BidObserver {
  /**
   * Gửi {@link ServerResponse} đến TẤT CẢ client đang kết nối.
   * Network layer tự lọc client nào đang xem item này.
   *
   * @param ServerResponse dữ liệu cần gửi — đã đóng gói trong DTO
   */
  void broadcastPriceUpdate(ServerResponse response);

  /**
   * Gửi thông báo phiên đấu giá kết thúc.
   *
   * @param broadcast dữ liệu kết thúc phiên
   */
  void broadcastAuctionEnded(ServerResponse response);
}