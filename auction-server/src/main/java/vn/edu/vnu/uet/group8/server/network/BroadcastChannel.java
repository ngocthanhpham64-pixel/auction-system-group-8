package vn.edu.vnu.uet.group8.server.network;

import vn.edu.vnu.uet.group8.common.dto.response.PriceUpdateBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;

/**
 * Interface làm cầu nối giữa Service layer và Network layer.
 *
 * Service chỉ gọi interface này — không biết gì về ClientHandler,
 * Socket, hay cách message được gửi đi.
 *
 * Network layer implement interface này theo cách riêng của mình.
 */
public interface BroadcastChannel {

  /**
   * Gửi thông báo giá mới đến TẤT CẢ client đang kết nối.
   * Network layer tự lọc client nào đang xem item này.
   *
   * @param broadcast dữ liệu cần gửi — đã đóng gói trong DTO
   */
  void broadcastPriceUpdate(PriceUpdateBroadcastResponse broadcast);

  /**
   * Gửi thông báo phiên đấu giá kết thúc.
   *
   * @param broadcast dữ liệu kết thúc phiên
   */
  void broadcastAuctionEnded(AuctionEndedBroadcastResponse broadcast);
}