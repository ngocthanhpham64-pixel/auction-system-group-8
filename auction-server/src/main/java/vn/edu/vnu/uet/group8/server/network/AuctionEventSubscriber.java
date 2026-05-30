package vn.edu.vnu.uet.group8.server.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.vnu.uet.group8.common.dto.model.NotificationDTO;
import vn.edu.vnu.uet.group8.common.dto.response.AuctionEndedBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.PriceUpdateBroadcastResponse;
import vn.edu.vnu.uet.group8.common.dto.response.ServerResponse;
import vn.edu.vnu.uet.group8.common.enums.EventType;
import vn.edu.vnu.uet.group8.common.enums.NotificationType;
import vn.edu.vnu.uet.group8.server.service.auction.AuctionEventBus;
import vn.edu.vnu.uet.group8.server.service.auction.event.AuctionEndedEvent;
import vn.edu.vnu.uet.group8.server.service.auction.event.BidPlacedEvent;
import vn.edu.vnu.uet.group8.server.service.user.NotificationService;

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
  private final NotificationService notificationService;

  public AuctionEventSubscriber(BroadcastChannel channel, NotificationService notificationService) {
    this.channel = channel;
    this.notificationService = notificationService;
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
        ServerResponse.broadcast(EventType.PRICE_UPDATE)
            .data(payload)
            .message("PRICE_UPDATE")
            .build();

    logger.info(
        "Broadcast PRICE_UPDATE: itemId={}, price={}, clients={}",
        event.getItemId(), event.getNewPrice(),
        channel.getConnectedClientCount());

    channel.broadcast(response);

    // Tự động sinh Notification cho người dùng bị vượt giá (nếu có)
    Integer prevBidderId = event.getPrevBidderId();
    if (prevBidderId != null && !prevBidderId.equals(event.getBidderId())) {
      try {
        NotificationDTO notif = notificationService.createNotification(
            prevBidderId,
            "Bị vượt giá!",
            "Giá của bạn tại sản phẩm #" + event.getItemId() + " vừa bị vượt qua. Hãy đặt giá mới để giành lại vị trí dẫn đầu!",
            NotificationType.OUTBID
        );
        NotificationDTO notifWithRelated = NotificationDTO.builder()
            .id(notif.getId())
            .userId(notif.getUserId())
            .title(notif.getTitle())
            .message(notif.getMessage())
            .type(notif.getType())
            .isRead(notif.isRead())
            .createdAt(notif.getCreatedAt())
            .relatedId(event.getItemId())
            .build();
        ServerResponse notifResponse = ServerResponse.broadcast(EventType.NOTIFICATION).data(notifWithRelated).build();
        
        channel.sendToUser(prevBidderId, notifResponse); // Gửi trực tiếp Socket đến chính chủ
      } catch (Exception e) {
        logger.error("Lỗi tạo thông báo OUTBID cho user {}: {}", prevBidderId, e.getMessage());
      }
    }
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
        ServerResponse.broadcast(EventType.AUCTION_ENDED)
            .data(payload)
            .message(message)
            .build();

    logger.info(
        "Broadcast AUCTION_ENDED: itemId={}, outcome={}, clients={}",
        event.getItemId(), event.getOutcome(),
        channel.getConnectedClientCount());

    channel.broadcast(response);

    // Tự động sinh Notification cho người thắng và người bán (nếu có)
    if (event.hasSold()) {
      try {
        Integer winnerId = event.getWinnerId(); 
        if (winnerId != null && winnerId > 0) {
          NotificationDTO notifWinner = notificationService.createNotification(
              winnerId,
              "Thắng đấu giá!",
              "Chúc mừng bạn đã thắng phiên đấu giá sản phẩm: " + event.getItemTitle() + " với giá " + event.getFinalPrice() + " VND.",
              NotificationType.AUCTION_WON
          );
          NotificationDTO notifWinnerWithRelated = NotificationDTO.builder()
              .id(notifWinner.getId())
              .userId(notifWinner.getUserId())
              .title(notifWinner.getTitle())
              .message(notifWinner.getMessage())
              .type(notifWinner.getType())
              .isRead(notifWinner.isRead())
              .createdAt(notifWinner.getCreatedAt())
              .relatedId(event.getItemId())
              .build();
          ServerResponse notifWinnerResponse = ServerResponse.broadcast(EventType.NOTIFICATION).data(notifWinnerWithRelated).build();
          channel.sendToUser(winnerId, notifWinnerResponse);
        }

        // Thêm thông báo cho người bán
        Integer sellerId = event.getSellerId();
        if (sellerId != null && sellerId > 0) {
          NotificationDTO notifSeller = notificationService.createNotification(
              sellerId,
              "Sản phẩm đã bán thành công!",
              "Sản phẩm '" + event.getItemTitle() + "' của bạn đã được chốt với giá " + event.getFinalPrice() + " VND. Số tiền đã được cộng vào ví.",
              NotificationType.SYSTEM
          );
          NotificationDTO notifSellerWithRelated = NotificationDTO.builder()
              .id(notifSeller.getId())
              .userId(notifSeller.getUserId())
              .title(notifSeller.getTitle())
              .message(notifSeller.getMessage())
              .type(notifSeller.getType())
              .isRead(notifSeller.isRead())
              .createdAt(notifSeller.getCreatedAt())
              .relatedId(event.getItemId())
              .build();
          ServerResponse notifSellerResponse = ServerResponse.broadcast(EventType.NOTIFICATION).data(notifSellerWithRelated).build();
          channel.sendToUser(sellerId, notifSellerResponse);
        }
      } catch (Exception e) {
        logger.error("Lỗi tạo thông báo AUCTION_WON/SYSTEM: {}", e.getMessage());
      }
    }
  }
}