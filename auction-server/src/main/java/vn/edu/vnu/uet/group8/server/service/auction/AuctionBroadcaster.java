package vn.edu.vnu.uet.group8.server.service.auction;

import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.PriceUpdateBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.network.BroadcastChannel;

/**
 * Đóng gói logic broadcast của hệ thống đấu giá.
 *
 * <p>Chịu trách nhiệm duy nhất: build {@link ServerResponse}
 * đúng format rồi ủy quyền cho {@link BroadcastChannel} gửi đi.
 * Không biết gì về Socket, ClientHandler hay GSON.
 *
 * <p>Hai loại broadcast:
 * <ul>
 *   <li>{@code PRICE_UPDATE} — khi có bid mới thành công
 *   <li>{@code AUCTION_ENDED} — khi phiên đấu giá kết thúc
 * </ul>
 */
public class AuctionBroadcaster {

  private static final Logger logger =
      LoggerFactory.getLogger(AuctionBroadcaster.class);

  // eventType phải khớp với chuỗi Client đăng ký qua
  // ResponseDispatcher.subscribe(eventType, listener)
  private static final String EVENT_PRICE_UPDATE  = "PRICE_UPDATE";
  private static final String EVENT_AUCTION_ENDED = "AUCTION_ENDED";

  private final BroadcastChannel channel;

  public AuctionBroadcaster(BroadcastChannel channel) {
    this.channel = channel;
  }

  // ════════════════════════════════════════════════════
  // BROADCAST KHI CÓ BID MỚI
  // ════════════════════════════════════════════════════

  /**
   * Broadcast giá mới đến tất cả client sau khi bid thành công.
   *
   * <p>Gọi bởi {@link AuctionService} SAU KHI đã unlock
   * {@code ReentrantLock} — tránh giữ lock trong khi gửi mạng.
   *
   * @param itemId         ID item vừa được đặt giá
   * @param newPrice       giá mới sau khi bid thành công
   * @param totalBids      tổng số lần bid tính đến thời điểm này
   * @param newEndTime     thời điểm kết thúc (có thể đã gia hạn do anti-sniping)
   * @param bidderUsername tên người vừa đặt giá — hiển thị trên UI
   * @param isExtended     {@code true} nếu anti-sniping vừa gia hạn thêm giờ
   */
  public void broadcastPriceUpdate(
      int itemId,
      BigDecimal newPrice,
      int totalBids,
      Instant newEndTime,
      String bidderUsername,
      boolean isExtended) {

    PriceUpdateBroadcastResponse payload =
        PriceUpdateBroadcastResponse.of(
            itemId, newPrice, totalBids,
            newEndTime, bidderUsername, isExtended);

    ServerResponse response =
        ServerResponse.broadcast(EVENT_PRICE_UPDATE)
            .data(payload)
            .message("Có giá mới cho sản phẩm #" + itemId)
            .build();

    logger.info(
        "Broadcast PRICE_UPDATE: itemId={}, newPrice={}, totalBids={}, "
            + "isExtended={}, clients={}",
        itemId, newPrice, totalBids, isExtended,
        channel.getConnectedClientCount());

    channel.broadcastToAll(response);
  }

  // ════════════════════════════════════════════════════
  // BROADCAST KHI PHIÊN KẾT THÚC CÓ NGƯỜI THẮNG
  // ════════════════════════════════════════════════════

  /**
   * Broadcast kết quả SOLD đến tất cả client.
   *
   * <p>Gọi bởi {@link AuctionClosingService} sau khi đã
   * chuyển trạng thái và chuyển tiền cho seller thành công.
   *
   * @param itemId         ID item vừa kết thúc
   * @param itemTitle      tiêu đề item để hiển thị thông báo
   * @param finalPrice     giá thắng cuối cùng
   * @param winnerUsername tên người thắng
   */
  public void broadcastAuctionSold(
      int itemId,
      String itemTitle,
      BigDecimal finalPrice,
      String winnerUsername) {

    AuctionEndedBroadcastResponse payload =
        AuctionEndedBroadcastResponse.sold(
            itemId, itemTitle, finalPrice, winnerUsername);

    ServerResponse response =
        ServerResponse.broadcast(EVENT_AUCTION_ENDED)
            .data(payload)
            .message(itemTitle + " đã được bán với giá " + finalPrice + " VND")
            .build();

    logger.info(
        "Broadcast AUCTION_ENDED (SOLD): itemId={}, winner={}, price={}, clients={}",
        itemId, winnerUsername, finalPrice,
        channel.getConnectedClientCount());

    channel.broadcastToAll(response);
  }

  // ════════════════════════════════════════════════════
  // BROADCAST KHI PHIÊN KẾT THÚC KHÔNG CÓ BID
  // ════════════════════════════════════════════════════

  /**
   * Broadcast kết quả ENDED_NO_BID đến tất cả client.
   *
   * @param itemId    ID item vừa kết thúc không có bid
   * @param itemTitle tiêu đề item để hiển thị thông báo
   */
  public void broadcastAuctionNoBid(int itemId, String itemTitle) {

    AuctionEndedBroadcastResponse payload =
        AuctionEndedBroadcastResponse.noBid(itemId, itemTitle);

    ServerResponse response =
        ServerResponse.broadcast(EVENT_AUCTION_ENDED)
            .data(payload)
            .message(itemTitle + " kết thúc không có người đặt giá")
            .build();

    logger.info(
        "Broadcast AUCTION_ENDED (NO_BID): itemId={}, clients={}",
        itemId, channel.getConnectedClientCount());

    channel.broadcastToAll(response);
  }
}