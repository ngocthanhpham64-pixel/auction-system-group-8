package vn.edu.vnu.uet.group8.server.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.PriceUpdateBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionEventBus;
import vn.edu.vnu.uet.group8.server.service.auction.event.AuctionEndedEvent;
import vn.edu.vnu.uet.group8.server.service.auction.event.BidPlacedEvent;

/**
 * Subscriber nhận domain event từ EventBus,
 * tự build ServerResponse rồi broadcast qua Socket.
 *
 * <p>Đây là nơi DUY NHẤT được phép import ServerResponse
 * trong toàn bộ luồng broadcast — nằm đúng tầng Network.
 *
 * <p>Service layer không biết class này tồn tại.
 * Service chỉ publish event vào EventBus rồi thôi.
 */
public class AuctionEventSubscriber {

  private static final Logger logger =
      LoggerFactory.getLogger(AuctionEventSubscriber.class);

  private static final String EVENT_PRICE_UPDATE  = "PRICE_UPDATE";
  private static final String EVENT_AUCTION_ENDED = "AUCTION_ENDED";

  // Đây mới là nơi hợp lý để có BroadcastChannel
  // vì đây đang ở tầng Network
  private final BroadcastChannel channel;

  public AuctionEventSubscriber(BroadcastChannel channel) {
    this.channel = channel;
  }

  /**
   * Đăng ký lắng nghe event từ EventBus.
   * Gọi một lần khi server khởi động.
   */
  public void registerTo(AuctionEventBus eventBus) {
    eventBus.subscribe(BidPlacedEvent.class,
        this::onBidPlaced);
    eventBus.subscribe(AuctionEndedEvent.class,
        this::onAuctionEnded);

    logger.info("AuctionEventSubscriber đã đăng ký vào EventBus");
  }

  // ════════════════════════════════════════════════════
  // HANDLER — nhận event, build response, broadcast
  // ════════════════════════════════════════════════════

  /**
   * Nhận BidPlacedEvent → build PriceUpdateBroadcastResponse
   * → broadcast đến tất cả client.
   *
   * <p>Đây là nơi duy nhất biết rằng BidPlacedEvent
   * tương ứng với response format "PRICE_UPDATE".
   */
  private void onBidPlaced(BidPlacedEvent event)
      throws Exception {

    PriceUpdateBroadcastResponse payload =
        PriceUpdateBroadcastResponse.of(
            event.getItemId(),
            event.getNewPrice(),
            event.getTotalBids(),
            event.getNewEndTime(),
            event.getBidderUsername(),
            event.isExtended());

    ServerResponse response =
        ServerResponse.broadcast(EVENT_PRICE_UPDATE)
            .data(payload)
            .message("PRICE_UPDATE")
            .eventType("PRICE_UPDATE")
            .build();

    logger.info(
        "Broadcast PRICE_UPDATE: itemId={}, price={}, clients={}",
        event.getItemId(), event.getNewPrice(),
        channel.getConnectedClientCount());

    channel.broadcast(response);
  }

  /**
   * Nhận AuctionEndedEvent → build AuctionEndedBroadcastResponse
   * → broadcast đến tất cả client.
   */
  private void onAuctionEnded(AuctionEndedEvent event)
      throws Exception {

    AuctionEndedBroadcastResponse payload = event.hasSold()
        ? AuctionEndedBroadcastResponse.sold(
              event.getItemId(), event.getItemTitle(),
              event.getFinalPrice(), event.getWinnerUsername())
        : AuctionEndedBroadcastResponse.noBid(
              event.getItemId(), event.getItemTitle());

    String message = event.hasSold()
        ? event.getItemTitle() + " đã được bán với giá "
              + event.getFinalPrice() + " VND"
        : event.getItemTitle()
              + " kết thúc không có người đặt giá";

    ServerResponse response =
        ServerResponse.broadcast(EVENT_AUCTION_ENDED)
            .data(payload)
            .message(message)
            .eventType("AUCTION_ENDED")
            .build();

    logger.info(
        "Broadcast AUCTION_ENDED: itemId={}, outcome={}, clients={}",
        event.getItemId(), event.getOutcome(),
        channel.getConnectedClientCount());

    channel.broadcast(response);
  }
}